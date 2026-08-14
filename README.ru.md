[![Maven Central](https://img.shields.io/maven-central/v/io.github.eugenprog/androidarc)](https://central.sonatype.com/artifact/io.github.eugenprog/androidarc)

# androidArc: Руководство пользователя

**androidArc** — минимальный слой MVI-архитектуры (Model-View-Intent) для Android + Jetpack Compose: базовый класс `ViewModel` и Compose-обвязка, которая связывает его с экраном. Никакого DI-фреймворка, никакой библиотеки навигации, никакой кодогенерации — только два строительных блока, которые нужны любому MVI-экрану.

[Read in English](README.md)

**Основные принципы:**
* **Минимальная поверхность:** два класса — `BaseMviViewModel` и `ScreenContainerWrapper`. Больше ничего учить не нужно.
* **Безопасность от крашей по конструкции:** исключение из обработчика action или из фоновой корутины логируется и гасится, а не улетает в хост-приложение.
* **Без привязок:** работает с любым DI и навигацией, которые у вас уже есть — androidArc зависит только от `androidx.lifecycle` и Compose runtime.

---

## 🚀 Установка

```kotlin
dependencies {
    // Актуальную версию смотрите в бейдже выше
    implementation("io.github.eugenprog:androidarc:<version>")
}
```

---

## ⚙️ Как пользоваться

### 1. Описываем контракт экрана

```kotlin
sealed interface CounterAction : UiAction {
    data object Increment : CounterAction
}

data class CounterState(val count: Int = 0) : UiState

sealed interface CounterEffect : UiEffect
```

### 2. Реализуем ViewModel

```kotlin
class CounterViewModel : BaseMviViewModel<CounterAction, CounterState, CounterEffect>(CounterState()) {
    override fun handleAction(action: CounterAction) {
        when (action) {
            is CounterAction.Increment -> updateState { it.copy(count = it.count + 1) }
        }
    }
}
```

`updateState` обновляет `state: StateFlow<CounterState>`. Для одноразовых событий (навигация, снэкбары) используйте `emitEffect` и подписывайтесь на `effects: Flow<CounterEffect>`. `wrappedRequest` выполняет suspend-вызов на IO и возвращает результат обратно на Main — стандартная форма для action, завязанного на use case — и возвращает запущенный `Job`, чтобы можно было отменить именно этот запрос (например, если новый поиск должен вытеснить предыдущий). Все job'ы `wrappedRequest` также отслеживаются внутри; `cancelAllRequests()` отменяет их все сразу, например из `onCleared()`. Опциональный колбэк `onFinal` всегда вызывается последним — при успехе, при ошибке и при отмене — удобно, чтобы гасить индикатор загрузки независимо от того, чем закончился запрос; если не передать его — ничего не меняется. `launchSafely` — это `viewModelScope.launch`, который при необработанном исключении логирует его вместо краша, точно так же, как `dispatch` делает это для самого `handleAction`.

### 3. Подключаем к экрану

```kotlin
@Composable
fun CounterContainer(viewModel: CounterViewModel = viewModel()) {
    ScreenContainerWrapper(
        viewModel = viewModel,
        onEffect = { /* обработка CounterEffect, например навигация */ },
        screenContent = { state, action ->
            CounterScreen(state = state, action = action)
        },
    )
}
```

`ScreenContainerWrapper` собирает `state` с учётом жизненного цикла и пробрасывает `effects` в `onEffect`, а затем рендерит `screenContent` с `(state, dispatch)`.

---

## ✨ Демо-приложение

Модуль `app` — небольшое приложение "Notes" из двух экранов (список → детали), которое прогоняет androidArc через реальный DI ([KoGen DI](https://github.com/KoGen-libs/KoGen-Di)) и реальную навигацию ([KoGen Navigation](https://github.com/KoGen-libs/KoGen-Navigation)) с кодогенерацией — и показывает, что сам androidArc при этом не завязан ни на то, ни на другое. Запустить:

```
./gradlew :app:installDebug
```

[README](README.md)
