# Frontend

...existing code...

## Developer tooling

This project uses ESLint, Prettier and Stylelint. Run `npm run lint` and `npm run format` to check
style and format code.

There are no git hooks. Husky, lint-staged and commitlint are in devDependencies but nothing is
configured, and there is no `prepare` script, so linting is enforced by CI rather than locally.