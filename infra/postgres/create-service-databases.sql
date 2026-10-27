CREATE ROLE auth_service_user LOGIN PASSWORD :'auth_password';
CREATE ROLE patient_service_user LOGIN PASSWORD :'patient_password';
CREATE ROLE appointment_service_user LOGIN PASSWORD :'appointment_password';
CREATE ROLE notification_service_user LOGIN PASSWORD :'notification_password';
CREATE ROLE audit_service_user LOGIN PASSWORD :'audit_password';

CREATE DATABASE auth_service OWNER auth_service_user;
CREATE DATABASE patient_service OWNER patient_service_user;
CREATE DATABASE appointment_service OWNER appointment_service_user;
CREATE DATABASE notification_service OWNER notification_service_user;
CREATE DATABASE audit_service OWNER audit_service_user;
