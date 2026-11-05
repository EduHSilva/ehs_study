# StudyFlow — Android

Cliente nativo Android do StudyFlow, em Kotlin + Jetpack Compose (Material 3). Consome a mesma API
do front web (`../web`), com login real, dashboard, tarefas/calendário, anotações, pomodoro,
projetos e revisão.

## Requisitos

- Android Studio (o projeto foi validado com o JBR 25) ou JDK 17+
- Android SDK 36 (compile/target) — `minSdk` 26
- Gradle wrapper incluso (`gradlew` / `gradlew.bat`)

## Configuração

As duas variáveis abaixo são lidas de `local.properties` (não versionado) ou do ambiente:

| Chave em `local.properties` | Variável de ambiente | Padrão |
| --- | --- | --- |
| `sdk.dir` | `ANDROID_HOME` | — |
| `studyflow.apiBaseUrl` | `STUDYFLOW_API_BASE_URL` | `http://10.0.2.2/` |
| `studyflow.googleWebClientId` | `STUDYFLOW_GOOGLE_WEB_CLIENT_ID` | vazio |

Exemplo de `local.properties`:

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
studyflow.apiBaseUrl=http://10.0.2.2/
studyflow.googleWebClientId=1234567890-abc.apps.googleusercontent.com
```

Notas:

- `10.0.2.2` é o `localhost` da máquina host vista de dentro do emulador. Em aparelho físico use o
  IP da máquina na rede local.
- HTTP em texto puro só é permitido no build `debug` (manifest overlay). Para produção, use HTTPS.
- O botão **Entrar com Google** só aparece quando `studyflow.googleWebClientId` está preenchido. O
  servidor precisa aceitar `tokenGmail` em `POST /users/auth/login` (mesmo contrato do web).
- Sem `studyflow.apiBaseUrl` configurado o app aponta para o emulador e falha se não houver backend
  em `http://10.0.2.2/`.

## Build, testes e execução

```bash
# Windows (PowerShell/cmd) — execute a partir desta pasta
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest

# Linux/macOS
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

O APK sai em `app/build/outputs/apk/debug/app-debug.apk` (relativo a esta pasta; o módulo fica em
`app/app`). Para instalar:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Ou abra a pasta `app/` no Android Studio e rode a configuração `app`.

## Arquitetura

Projeto Gradle único com o módulo `:app` (portanto o código do módulo fica em `app/app/src`).

```
app/app/src/main/java/com/studyflow/app/
├── MainActivity.kt              # tema/idioma/DI via CompositionLocal
├── StudyFlowApplication.kt
├── core/
│   ├── i18n/                    # Strings pt-BR/en (mesmas chaves do web)
│   ├── network/                 # OkHttp + Retrofit + envelope da API
│   ├── prefs/                   # SharedPreferences
│   ├── session/                 # token + usuário (chave studyflow-auth)
│   ├── settings/                # tema/idioma (studyflow-theme / studyflow-locale)
│   └── ui/                      # tema Compose e componentes compartilhados
├── data/
│   ├── model/                   # modelos de domínio
│   ├── remote/                  # DTOs e interfaces Retrofit
│   ├── mappers/                 # DTO → domínio (tolerante a campos ausentes)
│   └── repository/              # chamadas HTTP
├── domain/                      # regras puras (calendário, formatação, validação)
├── feature/<nome>/              # ViewModel + tela de cada feature
├── navigation/                  # NavHost, drawer e bottom bar
└── di/AppContainer.kt           # injeção manual de dependências
```

- **MVVM**: cada feature tem um `ViewModel` com `StateFlow` e as telas apenas observam/emitem.
- **Sessão**: `SessionManager` persiste token + usuário; qualquer resposta `401` expira a sessão e
  o gate de autenticação volta para o login. Chamadas de `/tasks` também enviam `X-User-ID`.
- **Portabilidade**: as regras de calendário (`domain/tasks/StudyCalendar.kt`), formatação de datas
  (`TaskFormat.kt`) e validação de formulários (`AuthValidation.kt`) são puras (sem Android) e
  cobertas por testes JVM.
- **Navegação**: Navigation Compose com drawer + bottom bar. A tela de revisão existe e compila,
  mas o item de menu está desabilitado com **EM BREVE**, igual ao web.

## Testes

Testes JVM (não precisam de emulador):

- `domain/tasks/StudyCalendarTest.kt` — grade semanal/mensal, tarefas únicas/diárias, conclusões.
- `domain/tasks/TaskFormatTest.kt` — formatação de prazos e períodos.
- `domain/auth/AuthValidationTest.kt` — e-mail, senha e nome de usuário.

```bash
./gradlew :app:testDebugUnitTest
```
