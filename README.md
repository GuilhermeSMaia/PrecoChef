# PrecoChef

Sistema de apoio à decisão de compras que compara os preços de produtos entre diferentes mercados, destacando a opção mais barata para ajudar o consumidor a economizar. O projeto é dividido em uma API REST em **Quarkus** e uma interface web em **Next.js/React**.

---

## Funcionalidades

- **Comparador de preços** — compara o mesmo produto entre diferentes mercados cadastrados e destaca a opção mais barata.
- **Cadastro de mercados** — criação, edição e listagem de mercados (supermercados) no sistema.
- **Cadastro de produtos** — criação e listagem de produtos, organizados por categoria e unidade de medida.
- **Definição de preços por mercado** — vínculo de um preço de produto a um mercado específico (`Precos`), permitindo múltiplos preços para o mesmo produto conforme o mercado.
- **Lista de compras** — montagem de uma lista de compras a partir dos produtos cadastrados.

> 🔎 **Webscraping (roadmap):** a proposta do produto é que a coleta dos preços nos mercados seja automatizada via webscraping. Na base de código atual, o cadastro de preços (`DefinirPreco`) é feito manualmente através da API/formulário — a automação da coleta é o próximo passo natural da evolução do projeto (ver seção Roadmap).

---

## 🛠️ Tecnologias utilizadas

### Back-end — API REST
- **[Quarkus](https://quarkus.io/)** (Java 21) — framework para a API REST, com *live reload* em modo dev.
- **Hibernate ORM with Panache** — camada de persistência e repositórios (`ProdutosRepository`, `MercadoRepository`, `PrecosRepository`, etc.).
- **Quarkus REST (RESTEasy Reactive) + JSON-B** — exposição dos endpoints e serialização JSON.
- **Flyway** — versionamento e migração do schema do banco de dados.
- **SmallRye OpenAPI** — documentação/contrato da API (Swagger).
- **MySQL** (via `quarkus-jdbc-mysql`) como banco relacional (driver PostgreSQL também presente no projeto).

### Front-end — Interface web
- **[Next.js](https://nextjs.org/)** (App Router) + **React** + **TypeScript**.
- **Tailwind CSS** + **shadcn/ui** (Radix UI) — componentes de interface.
- **React Hook Form** + **Zod** — formulários e validação.
- **Context API** (`shopping-list-context`) — estado da lista de compras no cliente.

---
```bash
cd BackEnd

# suba em modo desenvolvimento (live reload)
./mvnw quarkus:dev
```

A API sobe por padrão em `http://localhost:8080`, com a documentação OpenAPI/Swagger disponível em `/q/swagger-ui`.

### Front-end (Next.js)

```bash
cd FrontEnd

# instale as dependências (o repositório inclui lockfiles de pnpm e yarn — escolha um)
npm install   # ou pnpm install / yarn

npm run dev
```

A interface sobe por padrão em `http://localhost:3000` e consome a API do back-end.

---

## ⚙️ Configuração do banco de dados

O back-end lê a configuração em `BackEnd/src/main/resources/application.properties`:

```properties
quarkus.datasource.db-kind=mysql
quarkus.datasource.jdbc.url=jdbc:{DatabaseUrl}
quarkus.datasource.username={UsernameDB}
quarkus.datasource.password={SenhaDB}
```

---

## 🗺️ Roadmap

- [ ] **Webscraping automatizado** dos preços diretamente dos sites/apps dos mercados, substituindo o cadastro manual de preços.
- [ ] Agendamento periódico da coleta de preços (ex.: job/cron no Quarkus).
- [ ] Histórico de variação de preço por produto/mercado.
- [ ] Autenticação e controle de acesso ao painel administrativo.
- [ ] Mover credenciais do banco para variáveis de ambiente / secrets.
- [ ] Testes automatizados no back-end (Quarkus + REST Assured já configurados no `pom.xml`).
