# ODSOFT 2026-2027 — Projeto 1

Backend REST de gestão de biblioteca (Books, Genres, Authors, Readers, Lendings) em Spring Boot 3.2.5, usado como base para o pipeline CI/CD do Projeto 1 de ODSOFT.

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `psoft-project-2024-g1/` | Código da aplicação (Maven) |
| `docs/` | Enunciado, diagramas (`diagrams/`) e relatórios (`reports/`) |

## Requisitos

- JDK 17 ou superior (testado com JDK 21)
- Maven 3.9+

O `mvnw` do projeto não funciona (falta a pasta `.mvn/wrapper/`). Usar o `mvn` do sistema, ou regenerar o wrapper com `mvn wrapper:wrapper`.

## Correr a aplicação

Todos os comandos correm dentro de `psoft-project-2024-g1/`.

```bash
cd psoft-project-2024-g1
mvn spring-boot:run
```

Em alternativa, gerar o jar e executá-lo:

```bash
mvn clean package -DskipTests
java -jar target/psoft-g1-0.0.1-SNAPSHOT.jar
```

Com a aplicação a correr (porta 8080):

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui |
| OpenAPI (JSON) | http://localhost:8080/api-docs |
| Login | `POST /api/public/login` |

A base de dados é H2 em memória. O perfil `bootstrap` (ativo por defeito) cria dados de exemplo e utilizadores, por exemplo:

| Papel | Username | Password |
|---|---|---|
| Librarian | `maria@gmail.com` | `Mariaroberta!123` |
| Reader | `manuel@gmail.com` | `Manuelino123!` |

Há uma coleção Postman em `psoft-project-2024-g1/Docs/`.

## Testes e qualidade

| Comando | O que faz |
|---|---|
| `mvn clean test` | Testes unitários (Surefire) |
| `mvn clean verify` | Testes unitários e de integração, cobertura, SpotBugs, PMD e relatórios HTML |
| `mvn test-compile pitest:mutationCoverage` | Mutation testing (PIT), cerca de 2 minutos |
| `mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>` | Análise no SonarQube (precisa de um servidor SonarQube a correr) |

Usar sempre `clean`. Com classes antigas em `target/` (por exemplo, compiladas pelo IDE), o agente do JaCoCo pode fazer falhar os testes com `VerifyError: Control flow falls through code end`.

Os testes estão organizados por módulo e por nível, em `src/test/java/pt/psoft/g1/psoftg1/<módulo>/<nível>/`:

| Pasta | Nível | Doubles | Plugin |
|---|---|---|---|
| `unit/` | Unitário: uma classe isolada dos colaboradores | Mockito (dummy, stub, mock, spy) | Surefire |
| `integration/` | Integração: várias camadas reais (Spring, JPA, H2) | Só onde é preciso isolar uma parte (ex.: `AuthorService` no teste do controller) | Failsafe |
| `system/` | Sistema (e2e): aplicação completa numa porta aleatória, acedida só por HTTP | Nenhum | Failsafe |

O Failsafe corre o que está em `integration/` e `system/`; o Surefire corre o resto. Testes com `@Disabled("Known defect: ...")` documentam defeitos encontrados e ainda não corrigidos.

## Plugins Maven

| Plugin | Versão | Fase | Função | Relatório |
|---|---|---|---|---|
| `maven-surefire-plugin` | 3.2.5 | `test` | Testes unitários (exclui `integration/` e `system/`) | `target/surefire-reports/` |
| `maven-failsafe-plugin` | 3.2.5 | `integration-test`, `verify` | Testes de integração e de sistema (`integration/`, `system/`) | `target/failsafe-reports/` |
| `maven-surefire-report-plugin` | 3.2.5 | `verify` | Relatórios HTML dos testes unitários e de integração | `target/site/surefire-report.html`, `target/site/failsafe-report.html` |
| `jacoco-maven-plugin` | 0.8.12 | `initialize`, `verify` | Cobertura de código (unitários e integração no mesmo `jacoco.exec`) | `target/site/jacoco/index.html`, `target/site/jacoco/jacoco.xml` |
| `spotbugs-maven-plugin` | 4.8.6.4 | `verify` | Deteção de padrões de bugs | `target/spotbugsXml.xml`, `target/site/spotbugs.html` |
| `maven-pmd-plugin` | 3.25.0 | `verify` | Análise estática com o ruleset por defeito | `target/pmd.xml`, `target/reports/pmd.html` |
| `pitest-maven` + `pitest-junit5-plugin` | 1.16.2 + 1.2.1 | Manual | Mutation testing (só testes unitários) | `target/pit-reports/index.html`, `target/pit-reports/mutations.xml` |
| `sonar-maven-plugin` | 5.1.0.4751 | Manual | Envio da análise para o SonarQube (lê o `jacoco.xml`) | No servidor SonarQube |
