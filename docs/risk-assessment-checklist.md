# HIPAA Security and Privacy Risk Assessment Checklist

> **Assessment template, not an assessment or compliance attestation.** Every
> item below is unassessed until the covered entity/business associate assigns
> an accountable owner, reviews the actual deployment and evidence, records
> findings, and approves risk treatment. This software project cannot determine
> an organization's HIPAA status. Do not enter PHI while using this template
> or the development stack.

## How to use

For each item, record the deployment/environment, evidence reviewed, identified
threats and likelihood/impact, current safeguards, residual risk, treatment,
owner, due date, and approval. Mark an item **Not applicable** only with a
written rationale and reviewer approval. Reassess after significant changes,
incidents, or at the organization's defined review interval. Obtain qualified
privacy, security, legal, and compliance review.

**Current checklist status: Not assessed.** No deployment evidence, interviews,
formal risk ratings, or organizational approvals are supplied by this
repository.

## Administrative safeguards

- [ ] Assign security and privacy officials; document responsibilities and
  escalation paths.
- [ ] Perform and approve an organization-specific risk analysis covering
  confidentiality, integrity, and availability of ePHI across systems,
  people, facilities, vendors, and data flows.
- [ ] Maintain a risk-management plan with prioritized mitigations, owners,
  deadlines, and accepted residual risks.
- [ ] Approve workforce authorization, supervision, clearance, termination,
  sanctions, and minimum-necessary access procedures.
- [ ] Define unique user identification, access approval/review, emergency
  access, and periodic account/privilege recertification.
- [ ] Provide role-appropriate privacy/security awareness and training; retain
  completion records.
- [ ] Establish incident detection, reporting, triage, investigation,
  containment, recovery, and breach-notification procedures; conduct exercises.
- [ ] Review information-system activity, including access, authentication,
  administrative changes, failed operations, and audit-log alerts; document
  review cadence and escalation.
- [ ] Verify business associate relationships, BAAs, subcontractor flow-downs,
  vendor security evidence, and data-use terms before enabling services.
- [ ] Approve retention, archival, legal hold, secure disposal, and data
  minimization schedules for each data class.

## Physical safeguards

- [ ] Assess facility access, hosting locations, physical access monitoring,
  visitor handling, and environmental protections.
- [ ] Define workstation/device use, screen/privacy protections, endpoint
  security, and removable-media controls.
- [ ] Document device and media inventory, movement, reuse, sanitization, and
  destruction procedures.
- [ ] Review hosting provider controls and physical-security evidence for the
  actual production environment.

## Technical safeguards

- [ ] Enforce unique identities, strong authentication, MFA policy, session
  management, lockout/rate limits, and prompt deprovisioning. Test these
  controls against the production identity configuration.
- [ ] Validate least-privilege authorization and minimum-necessary access for
  every API, UI, background job, administrator, and service identity.
- [ ] Protect ePHI in transit with approved TLS/mTLS configurations; validate
  certificates, cipher policy, internal traffic, and renewal/revocation.
  **The included local Compose stack uses plaintext service connections.**
- [ ] Approve encryption at rest for databases, backups, logs, caches, queues,
  devices, and exported data; document key custody, rotation, recovery, and
  separation of duties. Application-field encryption in this code does not
  establish these deployment controls.
- [ ] Protect audit records from unauthorized access, alteration, and deletion;
  test event completeness, identity/time accuracy, retention, alerting, and
  review. The development implementation does not provide an atomic
  transaction/outbox for application changes and audit publication.
- [ ] Establish secure configuration, vulnerability/patch management,
  dependency scanning, code review, secrets management, and change control.
- [ ] Test input validation, authorization boundaries, API abuse protections,
  logging/redaction, and failure handling. Review all service and third-party
  integrations.
- [ ] Define backup frequency, encryption, access controls, retention, restore
  testing, and recovery objectives. Verify recovery across database, key,
  queue, and configuration dependencies.
- [ ] Review network segmentation, ingress/egress policy, service identity,
  monitoring, alerting, and availability protections.
- [ ] Evaluate any AI/automation feature for data exposure, access boundaries,
  output risks, retention, vendor use, and human oversight. The included
  assistant uses a local deterministic mock; changing providers requires
  separate legal and security approval.

## Privacy, breach response, and governance

- [ ] Identify permitted uses/disclosures, purpose limitations, and minimum
  necessary information for each user role and workflow.
- [ ] Define individual access, amendment, accounting, restriction, and
  communication workflows as applicable to the organization's obligations.
- [ ] Validate breach assessment, notification timelines, evidence preservation,
  and coordination responsibilities with counsel and affected parties.
- [ ] Document data inventory, data-flow diagrams, system boundaries, records
  of processing, and approved retention/deletion rules.
- [ ] Approve release criteria including independent security testing,
  integration/load testing, operational readiness, and signed risk acceptance.

## Findings and approvals

Record findings in the organization's approved risk register. For each
finding, include a unique ID, affected asset/data flow, threat scenario,
likelihood, impact, inherent/residual rating, existing evidence, corrective
action, accountable owner, target date, and approval/exception expiry.

| Assessment metadata | Value |
|---|---|
| Organization / environment | Not provided |
| Assessment owner / reviewers | Not assigned |
| Assessment date / review date | Not assessed |
| Evidence repository | Not provided |
| Approval / risk acceptance | Not approved |
