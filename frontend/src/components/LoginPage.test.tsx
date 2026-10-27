import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "../auth/AuthContext";
import { LoginPage } from "./LoginPage";

describe("LoginPage", () => {
  it("explains the patient and staff sign-in flow", () => {
    const queryClient = new QueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <AuthProvider><LoginPage /></AuthProvider>
      </QueryClientProvider>,
    );

    expect(screen.getByRole("heading", { name: "Sign in to Careflow" })).toBeInTheDocument();
    expect(screen.getByLabelText("Email address")).toBeRequired();
    expect(screen.getByLabelText("Password")).toHaveAttribute("minLength", "12");
    expect(screen.getByLabelText(/Authenticator code/)).toBeInTheDocument();
  });
});
