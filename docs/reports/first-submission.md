# Meta 1: estado inicial vs. estado atual

Comparação entre o estado inicial da aplicação (commit `ddddc2a`, 26-09-2026) e o estado atual (branch `dev`, 31-10-2026). O detalhe do estado inicial está em [sys-as-is/system-as-is.md](sys-as-is/system-as-is.md).

## 1. Estado inicial

### 1.1 Testes

Corriam **102 testes**, todos com sucesso, em 21 classes. Não havia testes de controllers, de segurança nem de sistema.

| Tipo | Classes | Testes |
|---|---:|---:|
| Unitários do modelo de domínio (value objects e entidades) | 14 | 89 |
| Integração (repositórios, serviços, arranque do contexto) | 5 | 13 |
| Sem nenhum teste a correr (`AuthorControllerIntegrationTest`, `TestAuthApi`) | 2 | 0 |

### 1.2 Cobertura

| Métrica | Valor |
|---|---:|
| Instruções (JaCoCo) | 13% |
| Branches (JaCoCo) | 4% (117/2 585) |
| Linhas (JaCoCo) | 19% (547/2 867) |
| Métodos (JaCoCo) | 17% |
| Mutation coverage (PIT) | 5% (143/3 134) |
| Test strength (PIT) | 63% |

Só o modelo de domínio tinha cobertura relevante. Controllers e serviços ficavam entre 0% e 3%.

### 1.3 Deploy

Não havia pipeline nem deploy: a aplicação só corria localmente, com H2 em memória.

![Deployment diagram do estado inicial](/docs/diagrams/system-as-is/deploy-diagram-sys-as-is.png)

## 2. Estado atual

| Indicador | Inicial | Atual |
|---|---:|---:|
| Testes executados | 102 | 419 (301 unitários, 118 de integração e sistema) |
| Classes de teste | 21 | 47 |
| Linhas cobertas (JaCoCo) | 19% | 72,6% |
| Branches cobertas (JaCoCo) | 4% | 21,3% |
| Mutation coverage (PIT) | 5% | 13% |
| Test strength (PIT) | 63% | 83% |

O deploy passou a ser feito por um pipeline Jenkins multibranch. As branches `dev` e `main` fazem deploy em DEV e, só em `main` e após aprovação manual com email, em PROD, com smoke tests e rollback se o health check falhar.

A solução planeada usava três máquinas virtuais: uma para o Jenkins, uma para DEV e outra para PROD, cada ambiente com o seu SGBD.

![Deployment diagram planeado](/docs/diagrams/system-to-be/deploy-diagram-sys-to-be.png)

Por problemas técnicos com as máquinas virtuais, os três ambientes foram simulados numa só máquina, cada um na sua porta:

| Ambiente planeado | Implementação | Porta |
|---|---|---:|
| `VM_JENKINS` | Jenkins instalado na máquina | 8080 |
| `VM_DEV` | Projeto Docker Compose `app-dev`, perfil `dev` | 8081 |
| `VM_PROD` | Projeto Docker Compose `app-prod`, perfil `prod` | 8082 |

Cada projeto Compose tem a sua rede e o seu container, por isso DEV e PROD continuam isolados um do outro. O SGBD de cada ambiente é o H2 em memória dentro do container da aplicação, por isso os dados perdem-se a cada deploy ou restart.

## 3. Testes criados nesta fase

Os testes estão em `src/test/java/pt/psoft/g1/psoftg1/<módulo>/<nível>/`. O nível decide o plugin que os corre.

| Nível | Classes | Plugin | Test doubles |
|---|---:|---|---|
| `unit/` | 30 | Surefire | Mockito (dummy, stub, mock, spy) e fakes |
| `integration/` | 13 | Failsafe | Só para isolar uma camada (`@MockBean`) |
| `system/` | 4 | Failsafe | Nenhum |

### 3.1 Unitário, caixa opaca, valores-limite

O título tem no máximo 128 caracteres: testa-se o limite e o valor logo acima (`TitleTest`).

```java
@Test
void ensureTitleWithMaxLengthIsAccepted() {
    assertEquals(128, new Title("a".repeat(128)).toString().length());
}

@Test
void ensureTitleAboveMaxLengthIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new Title("a".repeat(129)));
}
```

### 3.2 Unitário, caixa opaca, partição em classes de equivalência

Um representante de cada classe válida (ISBN-10 com dígito ou `X`, ISBN-13) e de cada classe inválida (dígito de controlo errado, letras, comprimentos errados) (`IsbnTest`).

```java
@ParameterizedTest
@ValueSource(strings = {"0306406152", "080442957X", "9780306406157", "9782826012092"})
void ensureValidIsbnIsAccepted(String valid) {
    assertEquals(valid, new Isbn(valid).toString());
}

@ParameterizedTest
@ValueSource(strings = {"0306406153", "9780306406158", "080442957x", "97803064061AB",
        "030640615", "03064061520", "978030640615", "97803064061570"})
void ensureInvalidIsbnIsRejected(String invalid) {
    assertThrows(IllegalArgumentException.class, () -> new Isbn(invalid));
}
```

### 3.3 Unitário, caixa transparente, condition coverage

Cada condição de `photo == null && photoURI != null || photo != null && photoURI == null` assume os dois valores (`AuthorServiceImplTest`).

```java
static Stream<Arguments> photoAndUriCombinations() {
    final MultipartFile file = mock(MultipartFile.class);
    return Stream.of(
            arguments(null, null, false),
            arguments(null, "photo.jpg", true),
            arguments(file, null, true),
            arguments(file, "photo.jpg", false));
}
```

### 3.4 Unitário com test doubles (stub e mock)

O repositório é um stub, que devolve três empréstimos em aberto, e um mock, que verifica que nada é gravado (`LendingServiceImplTest`).

```java
@Test
void createIsForbiddenWithThreeOutstandingLendings() {
    givenOutstanding(outstandingLendingStartedDaysAgo(1), outstandingLendingStartedDaysAgo(2),
            outstandingLendingStartedDaysAgo(3));

    assertThrows(LendingForbiddenException.class, () -> service.create(request));
    verify(lendingRepository, never()).save(any());
}
```

### 3.5 Integração: repositório e base de dados

A query corre contra o H2 real, sem doubles (`AuthorRepositoryIntegrationTest`, `@DataJpaTest`).

```java
@Test
public void whenSearchByNamePrefix_thenReturnMatchingAuthorsOnly() {
    entityManager.persist(new Author("Alexandre Pereira", "Professor", null));
    entityManager.persist(new Author("Alexandra Lucas", "Escritora", null));
    entityManager.persist(new Author("Manuel Pina", "Jornalista", null));
    entityManager.flush();

    assertThat(authorRepository.searchByNameNameStartsWith("Alexand"))
            .extracting(Author::getName)
            .containsExactlyInAnyOrder("Alexandre Pereira", "Alexandra Lucas");
}
```

### 3.6 Integração: serviço, repositórios e base de dados

Serviço, repositórios e H2 reais, com valores-limite na regra dos 3 empréstimos em aberto (`LendingServiceImplIntegrationTest`).

```java
@Test
void testCreateIsForbiddenAfterThreeOutstandingLendings() {
    var request = new CreateLendingRequest("9782826012092",
            LocalDate.now().getYear() + "/1");
    // boundary value analysis: the 3rd outstanding lending is allowed, the 4th is not
    for (int i = 0; i < 3; i++) {
        assertThat(lendingService.create(request).getReturnedDate()).isNull();
    }

    assertThrows(LendingForbiddenException.class, () -> lendingService.create(request));
    assertThat(lendingRepository.listOutstandingByReaderNumber(readerDetails.getReaderNumber())).hasSize(3);
}
```

### 3.7 Integração: HTTP, Spring Security e controller

O pedido passa pelo Spring MVC e pela configuração de segurança real; só o serviço é um `@MockBean`. Um leitor não vê o empréstimo de outro leitor (`LendingControllerIntegrationTest`).

```java
@Test
@WithMockUser(roles = Role.READER)
void readerCannotSeeTheLendingOfAnotherReader() throws Exception {
    given(lendingService.findByLendingNumber(LENDING)).willReturn(Optional.of(lendingOf(anaDetails)));
    givenLoggedIn(rui, ruiDetails);

    mockMvc.perform(get("/api/lendings/" + LENDING)).andExpect(status().isForbidden());
}
```

### 3.8 Sistema (end-to-end pela API)

A aplicação completa arranca numa porta aleatória e é usada só por HTTP, com login e JWT reais. Como não há frontend, estes testes fazem o papel dos e2e (`LendingApiSystemTest`).

```java
@Test
void readerCannotHaveMoreThanThreeOutstandingLendings() {
    final String librarian = login(client, LIBRARIAN, LIBRARIAN_PASSWORD);
    final String andre = YEAR + "/8";

    lend(librarian, andre).expectStatus().isCreated();
    lend(librarian, andre).expectStatus().isCreated();
    lend(librarian, andre).expectStatus().isCreated();
    lend(librarian, andre).expectStatus().isForbidden();
}
```

### 3.9 Smoke test

Confirma que a aplicação arranca e responde, antes de qualquer outro teste fazer sentido (`PsoftG1ApplicationIntegrationTest`).

```java
@Test
void healthEndpointIsUp() throws Exception {
    mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
}
```

### 3.10 Segurança

O HTML enviado pelo utilizador é sanitizado contra XSS (`StringUtilsCustomTest`).

```java
@ParameterizedTest
@ValueSource(strings = {
        "<script>alert(1)</script>Texto",
        "<img src=x onerror=alert(1)>Texto",
        "<iframe src=\"https://evil.test\"></iframe>Texto"})
void dangerousHtmlIsRemoved(String html) {
    assertEquals("Texto", StringUtilsCustom.sanitizeHtml(html));
}
```

### 3.11 Defeito documentado (regressão)

Os testes encontraram 14 defeitos no código inicial. Cada um fica num teste desativado que descreve o comportamento correto; ao corrigir o defeito, tira-se o `@Disabled` e o teste passa a proteger contra regressões (`PhoneNumberTest`).

```java
@Disabled("Known defect: PhoneNumber only checks the first digit and the length, so letters are accepted")
@Test
void ensureNonDigitsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("9abcdefgh"));
}
```
