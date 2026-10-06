# Agent integration chain

This tracker coordinates the approved six-PR integration for [CRM issue #12](https://github.com/ARTECTEAM/AR-CRM/issues/12). The feature-branch chain keeps the work reviewable while preserving the current `main` behavior.

## Delivery sequence

| PR | Work unit | Boundary |
|---|---|---|
| 1 | Conversation memory | Idempotent turn creation, durable memory, and visible conversation history/regeneration. |
| 2 | Trusted actors and action ledger | Spring AI trusted-actor propagation, actor-scoped actions, and safe tool-loop behavior. |
| 3 | CRM tools and WhatsApp retirement | Typed, bounded, grouped CRM tools; intentionally remove the legacy WhatsApp agent surfaces. |
| 4 | Role permissions | Fixed permission catalog and configurable grants enforced by CRM use cases and endpoints. |
| 5 | Capability-aware agent access | Filter available tools by the current user's capabilities and prove role behavior over HTTP. |
| 6 | Performance and Responses API | Add the scoped agent performance work and the Responses API integration. |

Each child targets its immediate predecessor; only the tracker branch is intended to merge to `main`, after all children have been reviewed and integrated.

```text
main
  └── tracker
      └── 📍 PR 1: conversation memory
          └── PR 2: trusted actors and action ledger
              └── PR 3: CRM tools and WhatsApp retirement
                  └── PR 4: role permissions
                      └── PR 5: capability-aware agent access
                          └── PR 6: performance and Responses API
```

## Preservation and review rules

- Preserve the current `main` pagination contracts and kanban behavior throughout the chain.
- The legacy WhatsApp agent-surface removal in PR 3 is intentional; do not conflate it with unrelated deletions.
- The cohesive feature is approved as a `size:exception`; keep each child focused and report its actual additions/deletions and verification results.
- Do not merge intermediate branches to `main`. Keep the tracker draft/no-merge until the complete chain has passed review and verification.
- Verification is pending for the assembled feature. Run the applicable tests for each child against its immediate parent and report results without claiming unrun checks; provider smoke tests remain opt-in.
