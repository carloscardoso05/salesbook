# AGENTS.md

## Comandos
- Gerenciador é **Bun** (`bun.lock`); não use npm/yarn.
- Dev: `bun run dev` · Testes: `bun run test` · Watch: `bun run test:watch`
- Teste único: `bunx vitest run tests/salesbook.spec.ts` ou `bunx vitest run -t "addOrderItem"`
- Verificação antes de finalizar: `bun run test && bun run typecheck && bun run build` (o build já roda `vue-tsc -b`)
- Ícones PWA: `bun run pwa:assets` regenera de `public/logo.svg` via `pwa-assets.config.ts`. Não editar os PNGs/`favicon.ico` à mão.

## Arquitetura
- PWA offline-first: Vue 3 + Vite + RxDB 17 sobre IndexedDB (`getRxStorageDexie`) + Tailwind 4.
- Coleções em `src/db/schemas.ts` (`customers`, `products`, `orders`, `orderitems`, `payments`). `createdAt` é ISO string com `format: 'date-time'`; IDs vêm de `crypto.randomUUID()`; `nameNormalized` é indexado (trim + espaços colapsados + lowercase pt-BR).
- `src/db/database.ts`: `getDatabase()` singleton do app; `createSalesbookDatabase(name)` para testes. Dev-mode e validação AJV só quando `import.meta.env.DEV`.
- `src/services/salesbook.ts` é a única porta de escrita com efeito colateral: views nunca chamam `insert/patch/remove` direto. Um mutex serializa operações; como o RxDB grátis não tem transação multi-coleção, cada operação valida antes, usa `incrementalModify` e compensa (rollback manual) em erro.
- Regras: adicionar `OrderItem` debita 1 do estoque (nunca negativo) e o preço do `balance`; remover estorna ambos; excluir `Order` reusa a exclusão de cada item (cascade com estorno) antes de remover a Order; pagamento credita `balance` e exige `amount > 0`. 1 OrderItem = 1 unidade física.
- Nomes de cliente/produto são únicos case-insensitive, garantido no service (`DuplicateNameError`), não por índice único do banco.
- `src/composables/useRxQuery.ts` transforma `RxQuery.$` em `Ref` de JSON. `useDatabase`/`useSalesbook` usam provide/inject; o provider fica em `src/main.ts`, que só monta o app após abrir o banco.

## Convenções
- UI em pt-BR (entidades/schema em inglês); moeda e datas via `Intl` em `src/utils/format.ts`.
- Tailwind v4 sem `tailwind.config.js`: tema em `src/style.css` (`@theme`, `@layer components` com `.btn`, `.card`, `.input`).
- `tsconfig` usa `erasableSyntaxOnly`: sem enums, parameter properties ou namespaces.
- Alterar schema RxDB já persistido exige bump de `version` + migration; em dev, apague o banco ou troque o nome.

## Testes
- Vitest + `fake-indexeddb/auto` (primeira linha do spec) porque Node não tem IndexedDB.
- Cada teste cria banco com nome único e roda `db.remove()` no `afterEach`; dev-mode do RxDB fica ativo.
- `tests/salesbook.spec.ts` cobre as regras de negócio; `tsconfig.app.json` inclui `tests/`, então `vue-tsc` valida os testes também.

## Deploy
- Repo: `carloscardoso05/salesbook-local` · Site: https://carloscardoso05.github.io/salesbook-local/
- `.github/workflows/deploy.yml` publica no GitHub Pages a cada push na `main`: testes, build com `BASE_PATH=/<repo>/`, `cp dist/index.html dist/404.html` (fallback de rotas SPA) e upload do artefato. Pages usa source "GitHub Actions".
- O `base` do Vite vem de `process.env.BASE_PATH` (default `/`); não hardcode o subpath.
- O remote é SSH. Push via HTTPS de arquivos em `.github/workflows/` falha porque o token do `gh` não tem o scope `workflow`.
