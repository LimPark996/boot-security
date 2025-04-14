**✅ GET / 요청 시 End to End 흐름 설명**

1. 사용자 (브라우저)
- 주소창에 http://localhost:8080/ 입력

2. MainController.index() 호출

```java
@GetMapping
public String index(Model model) {
    model.addAttribute("memoList", memoService.findAll());
    return "index";
}
```
- 컨트롤러가 요청을 받음
- memoService.findAll()로 서비스 레이어에 메모 전체 조회 요청

3. MemoServiceImpl.findAll()
- 서비스에서 Mapper에 작업 위임

4. MemoMapper.findAll()

```java
@Select("SELECT * FROM memo")
List<Memo> findAll();
```
- Supabase에 쿼리 전송 → 메모 목록 가져옴

5. 결과 반환 (DB → Mapper → Service → Controller)

6. model.addAttribute("memoList", 리스트)

- 메모 리스트를 모델에 담아서 View로 넘김

7. index.html (Thymeleaf) 렌더링

```html
<p th:text="${memoList}"></p>
```
- HTML에 데이터를 바인딩해 사용자에게 보여줌

🔄 전체 요청 흐름 요약

```plaintext
사용자
 ↓
MainController (index)
 ↓
MemoServiceImpl
 ↓
MemoMapper
 ↓
Supabase (DB)
 ↓
 ← memoList 반환
 ↓
Model에 담음
 ↓
Thymeleaf 렌더링 (index.html)
 ↓
사용자에게 응답
```

**✅ POST /add 요청 시 End to End 흐름 설명 (메모 추가)**

1. 사용자 (브라우저)

- /add 페이지에서 메모 내용을 작성하고 제출 (POST /add 요청 발생)

2. MainController.save() 호출

```java
@PostMapping("/add")
public String save(MemoForm form) throws Exception {
    Memo memo = Memo.fromText(form.getText());
    memoService.create(memo);
    return "redirect:/";
}
```

- MemoForm 객체에 사용자가 입력한 text 값이 자동 바인딩됨
- Memo.fromText()를 통해 Memo 객체 생성

3. Memo.fromText()

```java
public static Memo fromText(String text) {
    return new Memo(0L, text, "");
}
```

- 임시 ID와 createdAt을 넣은 Memo 객체 생성
- 이후 DB에서 자동으로 ID와 날짜가 채워질 예정

4. MemoServiceImpl.create()

```java
@Override
public void create(Memo memo) throws Exception {
    memoMapper.insert(memo);
}
```

- DB에 저장하는 작업을 MemoMapper에 위임

5. MemoMapper.insert()

```java
@Insert("INSERT INTO memo (text) VALUES (#{text})")
void insert(Memo memo);
```

- SQL 실행: memo 테이블에 text 필드만 삽입됨
- Supabase에서 id, created_at은 자동으로 처리됨

6. redirect:/

- 저장 후 홈 화면(/)으로 리다이렉트
- 다시 GET / 요청 발생 → 메모 목록 조회 후 index.html 렌더링


🔄 전체 요청 흐름 요약

```plaintext
🧑 사용자 (브라우저)
   ↓ (폼 데이터 전송: text)
MainController (POST /add)
   ↓
Memo.fromText()  ← 임시 생성자
   ↓
MemoServiceImpl.create()
   ↓
MemoMapper.insert()
   ↓
Supabase DB에 저장
   ↓
redirect:/ (GET /)로 돌아감 → 메모 목록 다시 조회
```


**✅ POST /delete-all 요청 시 End to End 흐름 설명 (전체 삭제)**

1. 사용자 (브라우저)
- index.html에서 "전체 삭제" 버튼 클릭 → POST /delete-all 요청

2. MainController.deleteAll() 호출

```java
@PostMapping("/delete-all")
public String deleteAll(RedirectAttributes redirectAttributes) throws Exception {
    memoService.deleteAll();
    redirectAttributes.addFlashAttribute("msg", "전체 삭제");
    return "redirect:/";
}
```

- 서비스에 전체 삭제 요청
- 삭제 메시지를 일회성 플래시 속성으로 추가

3. MemoServiceImpl.deleteAll()

```java
@Override
public void deleteAll() {
    memoMapper.deleteAll();
}
```

- 삭제 처리를 Mapper에게 위임

4. MemoMapper.deleteAll()

```java
@Delete("DELETE FROM memo")
void deleteAll();
```

- 모든 메모 삭제 SQL 실행
- Supabase의 memo 테이블 데이터가 전부 지워짐

5. redirect:/

- 삭제 완료 후 GET / 요청으로 리다이렉트

🔄 전체 요청 흐름 요약

```plaintext
🧑 사용자 (브라우저)
   ↓ (버튼 클릭)
MainController (POST /delete-all)
   ↓
MemoServiceImpl.deleteAll()
   ↓
MemoMapper.deleteAll()
   ↓
Supabase DB에서 전체 삭제
   ↓
redirect:/ (GET /)로 돌아감 → 메모 목록 조회 (비어 있음)
```

- 삭제 메시지가 index.html에서 p th:text="${msg}" 를 통해 출력됨
- 데이터는 없으므로 메모 리스트는 빈 상태로 표시됨

**✅ POST /delete/{id} 요청 시 End to End 흐름 설명 (메모 삭제)**

1. 🧑 사용자 (브라우저)

- "삭제" 버튼 클릭 → 해당 메모의 id를 포함한 POST /delete/{id} 요청 발생

2. 📍 MainController.delete() 호출

``` java
@PostMapping("/delete/{id}")
public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
    String msg = "%d를 정상적으로 삭제하였습니다.".formatted(id);
    redirectAttributes.addFlashAttribute("msg", msg);
    memoService.deleteById(id);
    return "redirect:/";
}
```

- URL에서 id 값을 PathVariable로 받음
- 삭제 메시지를 FlashAttribute로 모델에 임시 저장
- 서비스 계층에 deleteById(id) 요청

3. 📍 MemoServiceImpl.deleteById()

```java
@Override
public void deleteById(Long id) {
    memoMapper.deleteById(id);
}
```

- 실제 DB 작업은 Mapper에 위임

4. 📍 MemoMapper.deleteById()

```java
@Delete("DELETE FROM memo WHERE id = (#{id})")
void deleteById(Long id);
```

- 해당 id의 메모를 DB에서 삭제

5. 📍 redirect:/

- 삭제 후 홈 화면으로 리다이렉트

- → GET / 다시 요청되어 메모 목록 재조회

🔄 전체 요청 흐름 요약

```scss
🧑 사용자 (POST /delete/{id})
   ↓
MainController.delete()
   ↓
MemoServiceImpl.deleteById()
   ↓
MemoMapper.deleteById()
   ↓
Supabase DB 삭제
   ↓
redirect:/ → 다시 GET /
```

**✅ GET /update/{id} 요청 시 End to End 흐름 설명 (수정 폼 보여주기)**

1. 🧑 사용자 (브라우저)

- 메모 옆에 "수정" 버튼 클릭 → /update/1 같은 링크로 이동

2. 📍 MainController.update() 호출 (GET)

```java
@GetMapping("/update/{id}")
public String update(@PathVariable("id") Long id, Model model) {
    Memo memo = memoService.findById(id);
    model.addAttribute("memo", memo);
    return "update";
}
```

- URL에서 id 추출 → memoService.findById() 호출
- 조회된 메모를 모델에 memo 이름으로 담음
- update.html에 넘김

3. 📍 MemoServiceImpl.findById()

```java
@Override
public Memo findById(Long id) {
    return memoMapper.findById(id);
}
```

4. 📍 MemoMapper.findById()

```java
@Select("SELECT * FROM memo WHERE id = (#{id})")
Memo findById(Long id);
```

- DB에서 해당 id의 메모를 조회해서 반환

5. 📍 반환 후 → update.html 렌더링

- memo 객체가 화면에 출력됨 (form input에 들어감)

🔄 전체 요청 흐름 요약

```scss
🧑 사용자 (GET /update/{id})
   ↓
MainController.update() (GET)
   ↓
MemoServiceImpl.findById()
   ↓
MemoMapper.findById()
   ↓
Supabase에서 메모 조회
   ↓
update.html 렌더링 + memo 데이터 바인딩
```

**✅ POST /update/{id} 요청 시 End to End 흐름 설명 (메모 수정)**

1. 🧑 사용자 (브라우저)

- 수정 완료 후 "제출" 버튼 클릭 → form에 입력된 text와 함께 POST /update/{id} 요청

2. 📍 MainController.update() 호출 (POST)

```java
@PostMapping("/update/{id}")
public String update(@PathVariable("id") Long id, @RequestParam String text, RedirectAttributes redirectAttributes) {
    Memo oldMemo = memoService.findById(id);
    Memo newMemo = new Memo(oldMemo.id(), text, oldMemo.createdAt());
    memoService.update(newMemo);
    redirectAttributes.addFlashAttribute("msg", "정상적으로 수정되었습니다!");
    return "redirect:/";
}
```

- id로 기존 메모 조회
- 새로운 text를 반영한 새 Memo 객체 생성
- memoService.update() 호출

3. 📍 MemoServiceImpl.update()

```java
@Override
public void update(Memo newMemo) {
    memoMapper.update(newMemo);
}
```

4. 📍 MemoMapper.update()

```java
@Update("UPDATE memo SET text = (#{text}) WHERE id = (#{id})")
void update(Memo memo);
```

- id로 해당 메모의 text 필드를 업데이트

5. 📍 redirect:/

- 수정 후 홈 화면으로 돌아감
- → GET / 다시 요청되어 수정 결과가 반영된 메모 목록 출력

🔄 전체 요청 흐름 요약
```scss
🧑 사용자 (POST /update/{id})
   ↓ (입력값: text)
MainController.update() (POST)
   ↓
MemoServiceImpl.findById()
   ↓
MemoServiceImpl.update()
   ↓
MemoMapper.update()
   ↓
Supabase DB 수정
   ↓
redirect:/ → 다시 GET /
```
