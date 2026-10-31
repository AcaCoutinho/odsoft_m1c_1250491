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

Os relatórios desta secção e da secção 5 foram gerados com os plugins de medição adicionados no commit `02db4c2` (Surefire, Failsafe, JaCoCo, PIT e PMD). O código da aplicação e os testes são os originais. Nesse commit, as classes `*IntegrationTest` correm no Failsafe e as restantes no Surefire. Por isso, `LendingServiceImplTest` e `PsoftG1ApplicationTests` aparecem no relatório do Surefire, apesar de serem testes de integração.

| Plugin | Testes | Falhas | Tempo |
|---|---:|---:|---:|
| Surefire | 93 | 0 | 4,4 s |
| Failsafe | 9 | 0 | 5,8 s |

![Relatório Surefire](/docs/reports/sys-as-is/surefire-report.png)

![Relatório Failsafe](/docs/reports/sys-as-is/failsafe-report.png)

### 4.2 Cobertura

Cobertura medida com o JaCoCo, com os testes unitários e de integração juntos:

| Métrica | Cobertos | Total | Cobertura |
|---|---:|---:|---:|
| Instruções | 2 859 | 21 744 | 13% |
| Branches | 117 | 2 585 | 4% |
| Linhas | 547 | 2 867 | 19% |
| Métodos | 201 | 1 174 | 17% |
| Classes | 70 | 139 | 50% |

- **Modelo de domínio:** é a única camada com cobertura relevante, entre 55% (`usermanagement.model`) e 93% (`genremanagement.model`) das instruções.
- **Controllers (`api`) e serviços:** a cobertura fica entre 0% e 3%. As exceções são `lendingmanagement.services` (20%), `shared.services` (15%) e `authormanagement.services` (9%).
- **`configuration`:** tem 97%, mas só porque o contexto Spring arranca no `PsoftG1ApplicationTests`. Nenhum teste verifica estas classes (ver 4.3).
- **`bootstrapping`:** tem 0%, porque o perfil `bootstrap` não está ativo nos testes.

![Relatório JaCoCo](/docs/reports/sys-as-is/jacoco-report.png)

### 4.3 Eficácia (mutation testing)

Mutation testing com o PIT, só com os testes unitários (as classes `*IntegrationTest` estão excluídas):

| Métrica | Valor |
|---|---:|
| Classes analisadas | 123 |
| Cobertura de linhas | 19% (526/2 831) |
| Mutation coverage | **5%** (143/3 134) |
| Test strength | 63% (143/228) |

- **Mutation coverage de 5%:** 2 991 das 3 134 mutações não são detetadas. Destas, 2 906 estão em código que nenhum teste executa.
- **Test strength de 63%:** das 228 mutações em código executado pelos testes, 85 sobrevivem. Os testes executam esse código mas nem sempre verificam o resultado.
- **Camadas `api` e `services`:** 0% de mutation coverage em quase todos os pacotes. A exceção é `lendingmanagement.services` (8%).
- **`configuration`:** 99% de cobertura de linhas, mas só 7% de mutation coverage e 8% de test strength. Confirma que a cobertura vem só do arranque do contexto.
- **Modelo:** entre 11% (`usermanagement.model`) e 71% (`genremanagement.model`) de mutation coverage.

![Relatório PIT](/docs/reports/sys-as-is/pit-test-coverage-report.png)

## 5. Indicadores de qualidade

### 5.1 Análise estática (PMD)

O PMD, com o ruleset por defeito do `maven-pmd-plugin`, encontra **63 violações em 28 ficheiros**:

| Prioridade | Violações |
|---|---:|
| 2 | 1 |
| 3 | 5 |
| 4 | 57 |

| Regra | Violações |
|---|---:|
| `UnnecessaryImport` | 45 |
| `UselessParentheses` | 12 (11 no `Bootstrapper`) |
| `UnusedLocalVariable` | 2 |
| `UnnecessarySemicolon` | 1 |
| `AvoidBranchingStatementAsLastInLoop` | 1 |
| `UnusedPrivateMethod` | 1 |
| `UnusedPrivateField` | 1 |

A violação mais grave (prioridade 2) é um `AvoidBranchingStatementAsLastInLoop` em `shared/services/FileStorageService.java:135`. O mesmo ficheiro tem mais duas de prioridade 3: uma variável local e um método privado que não são usados. As restantes são sobretudo imports não usados, ou seja, código morto sem impacto funcional.

![Relatório PMD](/docs/reports/sys-as-is/pmd-report.png)

