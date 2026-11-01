# Meta 1: estado inicial vs. estado atual

Estado inicial: commit `ddddc2a` (26-09-2026), detalhado em [system-as-is.md](sys-as-is/system-as-is.md). Estado atual: branch `dev` (31-10-2026), detalhado em [system-to-be.md](sys-to-be/system-to-be.md).

## 1. Testes e cobertura

No início corriam 102 testes em 21 classes: 89 unitários do modelo de domínio e 13 de integração. Duas classes não tinham nenhum teste a correr, e não havia testes de controllers, de segurança nem de sistema.

| Indicador | Inicial | Atual |
|---|---:|---:|
| Testes executados | 102 | 419 (301 unitários, 104 de integração, 14 de sistema) |
| Classes de teste | 21 | 47 |
| Linhas cobertas (JaCoCo) | 19% | 72,6% |
| Branches cobertas (JaCoCo) | 4% | 21,3% |
| Mutation coverage (PIT) | 5% | 13% |
| Test strength (PIT) | 63% | 83% |

## 2. Deploy

No início não havia pipeline nem deploy: a aplicação só corria localmente.

![Deployment diagram do estado inicial](/docs/diagrams/system-as-is/deploy-diagram-sys-as-is.png)

Agora um pipeline Jenkins faz deploy em DEV (branches `dev` e `main`) e, após aprovação manual com email, em PROD (só `main`). A solução planeada usava três VMs:

![Deployment diagram planeado](/docs/diagrams/system-to-be/deploy-diagram-sys-to-be.png)

Por problemas técnicos com as VMs, os ambientes foram simulados numa só máquina:

| Planeado | Implementado | Porta |
|---|---|---:|
| `VM_JENKINS` | Jenkins | 8080 |
| `VM_DEV` | Docker Compose `app-dev`, perfil `dev` | 8081 |
| `VM_PROD` | Docker Compose `app-prod`, perfil `prod` | 8082 |

## 3. Testes criados nesta fase

A pasta define o nível: `unit/` (Surefire), `integration/` e `system/` (Failsafe).

### 3.1 Unitário, caixa opaca, valores-limite (`TitleTest`)

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

### 3.2 Unitário, caixa opaca, classes de equivalência (`IsbnTest`)

```java
@ParameterizedTest
@ValueSource(strings = {"0306406153", "9780306406158", "080442957x", "97803064061AB",
        "030640615", "03064061520", "978030640615", "97803064061570"})
void ensureInvalidIsbnIsRejected(String invalid) {
    assertThrows(IllegalArgumentException.class, () -> new Isbn(invalid));
}
```

### 3.3 Unitário, caixa transparente, condition coverage (`AuthorServiceImplTest`)

As 4 combinações de `photo == null && photoURI != null || photo != null && photoURI == null`:

```java
return Stream.of(
        arguments(null, null, false),
        arguments(null, "photo.jpg", true),
        arguments(file, null, true),
        arguments(file, "photo.jpg", false));
```

### 3.4 Unitário com stub e mock (`LendingServiceImplTest`)

O repositório devolve três empréstimos em aberto (stub) e verifica-se que nada é gravado (mock):

```java
givenOutstanding(outstandingLendingStartedDaysAgo(1), outstandingLendingStartedDaysAgo(2),
        outstandingLendingStartedDaysAgo(3));

assertThrows(LendingForbiddenException.class, () -> service.create(request));
verify(lendingRepository, never()).save(any());
```

### 3.5 Integração: repositório e H2 (`AuthorRepositoryIntegrationTest`)

```java
assertThat(authorRepository.searchByNameNameStartsWith("Alexand"))
        .extracting(Author::getName)
        .containsExactlyInAnyOrder("Alexandre Pereira", "Alexandra Lucas");
```

### 3.6 Integração: serviço, repositórios e H2 (`LendingServiceImplIntegrationTest`)

```java
// boundary value analysis: the 3rd outstanding lending is allowed, the 4th is not
for (int i = 0; i < 3; i++) {
    assertThat(lendingService.create(request).getReturnedDate()).isNull();
}
assertThrows(LendingForbiddenException.class, () -> lendingService.create(request));
```

### 3.7 Integração: HTTP, Spring Security e controller (`LendingControllerIntegrationTest`)

Só o serviço é um `@MockBean`:

```java
@Test
@WithMockUser(roles = Role.READER)
void readerCannotSeeTheLendingOfAnotherReader() throws Exception {
    given(lendingService.findByLendingNumber(LENDING)).willReturn(Optional.of(lendingOf(anaDetails)));
    givenLoggedIn(rui, ruiDetails);

    mockMvc.perform(get("/api/lendings/" + LENDING)).andExpect(status().isForbidden());
}
```

### 3.8 Sistema, end-to-end pela API (`LendingApiSystemTest`)

Aplicação completa, login e JWT reais, sem doubles:

```java
lend(librarian, andre).expectStatus().isCreated();
lend(librarian, andre).expectStatus().isCreated();
lend(librarian, andre).expectStatus().isCreated();
lend(librarian, andre).expectStatus().isForbidden();
```

### 3.9 Smoke test (`PsoftG1ApplicationIntegrationTest`)

```java
mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
```

### 3.10 Segurança, sanitização contra XSS (`StringUtilsCustomTest`)

```java
@ValueSource(strings = {
        "<script>alert(1)</script>Texto",
        "<img src=x onerror=alert(1)>Texto",
        "<iframe src=\"https://evil.test\"></iframe>Texto"})
void dangerousHtmlIsRemoved(String html) {
    assertEquals("Texto", StringUtilsCustom.sanitizeHtml(html));
}
```

### 3.11 Defeito documentado, futuro teste de regressão (`PhoneNumberTest`)

Os testes encontraram 14 defeitos; cada um fica desativado até ser corrigido:

```java
@Disabled("Known defect: PhoneNumber only checks the first digit and the length, so letters are accepted")
@Test
void ensureNonDigitsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("9abcdefgh"));
}
```
