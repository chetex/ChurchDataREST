---
name: User wants line-by-line English comments on new code
description: For new deliverables the user explicitly asks for every line to be documented with simple English comments.
type: feedback
---

When producing new code for this project, annotate virtually every statement with a short English comment explaining intent.

**Why:** User stated verbatim "Recuerda documentar el código línea por línea en inglés con comentarios sencillos." Overrides the repo-wide default of minimising comments.

**How to apply:** Whenever writing new Java classes in this repo, add concise English inline comments on each field, constructor, method and non-trivial statement. Keep them short and practical — avoid restating obvious syntax, but do explain the purpose of each line. Does not apply to one-line tweaks or cosmetic refactors where existing comment style already matches the surrounding code.
