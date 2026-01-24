## 키보드 활성화 시 상단 스크롤 제한 및 비정상적인 하단 공백

### 기존 코드
```kotlin
@Composable
fun SumCalculatorScreen(
    viewModel: SumCalculatorViewModel = hiltViewModel(),
    onNavigationBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("항목별 합산 계산기", style = MaterialTheme.typography.headlineSmall)

        TextButton(onClick = { viewModel.clearAllInputs() }) {
            Text("전체 비우기", color = androidx.compose.ui.graphics.Color.Red)
        }

        // ViewModel의 리스트를 순회하며 UI 생성
        viewModel.stockItems.forEach { item ->
            StockInputRow(
                item = item,
                onValueChange = { newValue ->
                    viewModel.onInputChanged(item.id, newValue)
                }
            )
        }
    }
```

#### 문제 1
- 기존 코드에서는 `enableEdgeToEdge()`도 고려하지 않았음
    - 그 결과 시스템 바에 의해서 UI가 가려지는 현상 발생

**해결책** : scaffold를 적용함으로써 시스템 바 영역을 확보.
    - innerPadding이 시스템 바 영역을 제외한 패딩값
    - column에 innerPadding 적용

#### 문제 2
- 가장 하단의 입력 필드에 포커스되어 키보드가 나타났을 때 상단 스크롤 제한

**해결책** : Column에 imePadding() 사용
    - 키보드가 올라올 때 레이아웃의 하단이 키보드 위까지 밀려 올라가고, 스크롤 가능한 영역이 계산됨
    - 추가적으로 AndroidManifest.xml에 android:windowSoftInputMode="adjustResize" 추가

```kotlin
@Composable
fun SumCalculatorScreen(
    viewModel: SumCalculatorViewModel = hiltViewModel(),
    onNavigationBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("항목별 합산 계산기", style = MaterialTheme.typography.headlineSmall)

            TextButton(onClick = { viewModel.clearAllInputs() }) {
                Text("전체 비우기", color = androidx.compose.ui.graphics.Color.Red)
            }

            // ViewModel의 리스트를 순회하며 UI 생성
            viewModel.stockItems.forEach { item ->
                StockInputRow(
                    item = item,
                    onValueChange = { newValue ->
                        viewModel.onInputChanged(item.id, newValue)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```
