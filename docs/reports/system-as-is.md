# System-as-is

## 1. Implementação

### 1.1 Stack

| Área | Tecnologia |
|---|---|
| Linguagem / runtime | Java 17 |
| Framework | Spring Boot 3.2.5 (Web, Data JPA, Validation, WebFlux `WebClient`) |
| Persistência | H2 em memória, schema gerado pelo Hibernate (`ddl-auto=update`) |
| Segurança | Spring Security, OAuth2 Resource Server com JWT assinado com chaves RSA |
| Documentação da API | springdoc-openapi 2.3.0 (Swagger UI) |
| Mapeamento DTO ↔ modelo | MapStruct 1.5.5 |
| Boilerplate | Lombok 1.18.30 |
| Outros | OWASP Java HTML Sanitizer; cliente para a API externa API Ninjas |

### 1.2 Arquitetura

Monólito REST com um único módulo Maven. O código está organizado por módulos de domínio, e cada um tem as camadas `api` (controllers e DTOs), `services`, `model` e `repositories`/`infrastructure`.

| Módulo | Ficheiros Java |
|---|---:|---:|
| `readermanagement` | 24 |
| `lendingmanagement` | 20 |
| `bookmanagement` | 19 | 
| `usermanagement` | 15 |
| `bootstrapping` | 2 |
| `shared` | 20 |
| `authormanagement` | 15 |
| `genremanagement` | 15 |
| `exceptions` | 5 |
| `configuration` | 4 |
| `auth` | 2 |
| `external` | 2 |
| raiz (`PsoftG1Application`) | 1 
| **Total** | **144** |


A API expõe **50 endpoints REST** em 7 controllers:

| Controller | Endpoints |
|---|---:|
| `ReaderController` | 13 |
| `BookController` | 10 |
| `AuthorController` | 9 |
| `LendingController` | 7 |
| `UserAdminApi` | 5 |
| `GenreController` | 4 |
| `AuthApi` (`/api/public/login`, `/api/public/register`) | 2 |

## 2. Deployment

Não existe uma estratégio de deploy para o projeto atualmente. O projeto apenas corre locamente.

![Deploy Diagram System as Is](/docs/diagrams/system-as-is/deploy-diagram-sys-as-is.png)

## 3. Práticas de build e de teste

### 3.1 Build

- **Maven:** o projeto herda de `spring-boot-starter-parent` 3.2.5. Só tem dois plugins declarados:
  - `spring-boot-maven-plugin`, que gera o fat jar (`repackage`);
  - `maven-compiler-plugin`, com os annotation processors do Lombok e do MapStruct.
- **Surefire:** não está declarado. Corre na versão 3.1.2 gerida pelo parent, com a configuração por defeito.
- **Maven Wrapper:** o `mvnw` não funciona, porque falta a pasta `.mvn/wrapper/` e o ficheiro não tem permissão de execução. A versão do Maven depende de cada máquina.
- **Dependências:** a dependência `junit` (JUnit 4) está declarada mas não é usada, porque todos os testes são JUnit 5.
- **Tempo:** `mvn clean package` demora **7,2 s** e corre 102 testes (com as dependências já em cache).


## 4. Testes: quantidade, cobertura e eficácia

### 4.1 Quantidade

O projeto tem 21 classes de teste e 2 classes utilitárias (`testutils`). São executados **102 testes**, todos com sucesso.

| Classe | Nível | Testes executados |
|---|---|---:|
| `LendingTest` | Unitário (domínio) | 14 |
| `AuthorTest` | Unitário (domínio) | 9 |
| `ReaderTest` | Unitário (domínio, com mocks) | 9 |
| `LendingNumberTest` | Unitário (domínio) | 8 |
| `IsbnTest` | Unitário (domínio) | 7 |
| `TitleTest` | Unitário (domínio) | 7 |
| `BookTest` | Unitário (domínio) | 6 |
| `NameTest` | Unitário (domínio) | 6 |
| `BioTest` | Unitário (domínio) | 5 |
| `DescriptionTest` | Unitário (domínio) | 4 |
| `GenreTest` | Unitário (domínio) | 4 |
| `BirthDateTest` | Unitário (domínio) | 4 |
| `PhoneNumberTest` | Unitário (domínio) | 4 |
| `PhotoTest` | Unitário (domínio) | 2 |
| `LendingRepositoryIntegrationTest` | Integração (`@DataJpaTest`) | 7 |
| `LendingServiceImplTest` | Integração (`@SpringBootTest`) | 3 |
| `PsoftG1ApplicationTests` | Integração (arranque do contexto) | 1 |
| `AuthorRepositoryIntegrationTest` | Integração (`@DataJpaTest`) | 1 |
| `AuthorServiceImplIntegrationTest` | Integração (`@SpringBootTest`) | 1 |
| `AuthorControllerIntegrationTest` | Integração | **0** (não tem nenhum `@Test`) |
| `TestAuthApi` | Integração | **0** (o ficheiro está todo comentado) |

**Resumo:** 89 testes unitários e 13 de integração.

