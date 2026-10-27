import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError, request } from "../api/client";
import type { Role, TokenResponse } from "../api/types";

interface Session {
  accessToken: string;
  refreshToken: string;
  role: Role;
}

interface AuthContextValue {
  session: Session | null;
  signIn: (email: string, password: string, mfaCode?: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  logoutError: string;
  api: <T>(path: string, options?: RequestInit) => Promise<T>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<Session | null>(null);
  const [refreshing, setRefreshing] = useState<Promise<Session> | null>(null);
  const [logoutError, setLogoutError] = useState("");

  const setTokens = useCallback((tokens: TokenResponse) => {
    const next = {
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      role: tokens.role,
    };
    setLogoutError("");
    setSession(next);
    return next;
  }, []);

  const signIn = useCallback(
    async (email: string, password: string, mfaCode?: string) => {
      const tokens = await request<TokenResponse>("/auth/login", undefined, {
        method: "POST",
        body: JSON.stringify({ email, password, mfaCode: mfaCode || null }),
      });
      setTokens(tokens);
    },
    [setTokens],
  );

  const register = useCallback(
    async (email: string, password: string) => {
      const tokens = await request<TokenResponse>("/auth/register", undefined, {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setTokens(tokens);
    },
    [setTokens],
  );

  const signOut = useCallback(async () => {
    const current = session;
    setSession(null);
    queryClient.clear();
    setLogoutError("");
    if (current) {
      try {
        await request<void>("/auth/logout", current.accessToken, {
          method: "POST",
          body: JSON.stringify({ refreshToken: current.refreshToken }),
        });
      } catch (error) {
        setLogoutError(error instanceof Error ? error.message : "Could not revoke the session.");
        throw error;
      }
    }
  }, [queryClient, session]);

  const api = useCallback(
    async <T,>(path: string, options: RequestInit = {}): Promise<T> => {
      const current = session;
      if (!current) throw new ApiError("Please sign in again.", 401);
      try {
        return await request<T>(path, current.accessToken, options);
      } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 401) throw error;
        let nextSession = refreshing;
        if (!nextSession) {
          nextSession = request<TokenResponse>("/auth/refresh", undefined, {
            method: "POST",
            body: JSON.stringify({ refreshToken: current.refreshToken }),
          })
            .then((tokens) => setTokens(tokens))
            .catch((refreshError: unknown) => {
              setSession(null);
              queryClient.clear();
              throw refreshError;
            });
          setRefreshing(nextSession);
          void nextSession.finally(() => setRefreshing(null)).catch(() => undefined);
        }
        const refreshed = await nextSession;
        return request<T>(path, refreshed.accessToken, options);
      }
    },
    [queryClient, refreshing, session, setTokens],
  );

  const value = useMemo(
    () => ({ session, signIn, register, signOut, logoutError, api }),
    [session, signIn, register, signOut, logoutError, api],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used within AuthProvider");
  return value;
}
