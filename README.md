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

---

**✅ (타임리프)전체 구조부터 먼저 보기**

- 이건 웹사이트 화면이에요.
- 그 화면에는 **버튼도 있고, 글 목록도 있고**, 글을 고치거나 지우는 기능도 있어요.
- 그런데 이 화면은 그냥 HTML만으로 만들지 않고 **타임리프(Thymeleaf)** 라는 도구를 써요.
- 왜냐면: 우리가 만든 서버(Spring Boot)에서 보낸 글 데이터를 화면에 보여줘야 하니까요.

---

```html
<form th:method="post" th:action="@{/delete-all}">
```

🟡 의미:
- "전체 삭제" 버튼을 눌렀을 때
- **POST 방식**으로 ( = 정보를 숨겨서 )
- **/delete-all 주소**로 서버에 요청을 보낼 거예요.

🔵 진짜 쉬운 말:
> "나 글 다 지우고 싶어!" 하고 서버에 말하는 버튼이에요.

---

```html
<a th:href="@{/add}">등록</a>
```

🟡 의미:
- `등록`이라는 글자를 누르면
- **/add 주소로 이동**해요 (즉, 새 글 쓰러 가는 화면으로 바뀜)

🔵 진짜 쉬운 말:
> "새 글 쓸래요!" 하고 새 창으로 넘어가는 버튼이에요.

---

```html
<p th:text="${msg}"></p>
```

🟡 의미:
- 서버가 보내준 `msg`라는 내용을 화면에 보여줘요.
- 글을 잘 썼거나 삭제했을 때 **"삭제 완료!"** 같은 메시지가 여기에 나와요.

🔵 진짜 쉬운 말:
> "방금 뭐 했는지 알려주는 메시지 자리에요."

---

```html
<ul>
         <li th:each="memo : ${memoList}">
             <span th:text="${memo.text}"></span>
             <a th:href="@{/update/{id}(id=${memo.id})}">수정</a>
             <!-- 갑자기 잘 안 되면 수파베이스를 리셋할 것! -->
             <form th:method="post" th:action="@{/delete/{id}(id=${memo.id()})}"><button>삭제</button></form>
         </li>
    </ul>
```

**설명**

---

```html
<li th:each="memo : ${memoList}">
```

🟡 의미:
- 서버가 보내준 메모들을 한 줄씩 꺼내서 반복해요.
- `memoList`는 전체 글 목록
- `memo`는 그중 하나하나의 글이에요

🔵 진짜 쉬운 말:
> "내가 가진 모든 메모를 한 줄씩 보여줘!" 하는 반복문이에요.

---

```html
<span th:text="${memo.text}"></span>
```

🟡 의미:
- 메모 하나에 담긴 `text` 내용만 보여줘요

🔵 진짜 쉬운 말:
> "여기에 진짜 메모 글 내용이 나와요!"

---

```html
<a th:href="@{/update/{id}(id=${memo.id})}">수정</a>
```

🟡 의미:
- `수정`을 누르면 메모의 ID를 넣어서 **/update/3 같은 주소로 이동**해요

🔵 진짜 쉬운 말:
> "이 글 고치고 싶어!" 하고 수정창으로 가는 버튼이에요.

---

```html
<form th:method="post" th:action="@{/delete/{id}(id=${memo.id()})}">
```

🟡 의미:
- 이 글 하나만 지우고 싶을 때 쓰는 폼이에요.
- `/delete/3` 같은 주소로 삭제 요청을 보냄

🔵 진짜 쉬운 말:
> "이 글만 지워줘!" 하는 버튼이에요.

---

**🧠 타임리프 핵심 표현 요약**

| 표현 | 쉽게 설명하면 |
|------|-----------------|
| `th:method="post"` | "서버야, 나 정보를 보내려고 해 (숨김 방식으로!)" |
| `th:action="@{/주소}"` | "서버야, 이 주소로 요청을 보낼게!" |
| `th:href="@{/주소}"` | "클릭하면 이 주소로 이동할게!" |
| `${변수}` | "서버에서 받은 데이터야!" |
| `th:text="${변수}"` | "이 데이터 글씨로 화면에 보여줘!" |
| `th:each="item : ${list}"` | "목록이야! 하나씩 꺼내서 화면에 보여줘!" |

---

**✅ 메모 생성 페이지**

```html
    <section>
        <form th:action="@{/add}" th:method="post" th:object="${memoForm}">
            <label>내용 :
                <input th:field="*{text}">
            </label>
            <button>등록</button>
        </form>
    </section>
```

**설명**

```html
<form th:action="@{/add}" th:method="post" th:object="${memoForm}">
🟡 의미:
- 폼(form)을 만들어서 서버에 메모를 보낼 거예요.
- 서버 주소는 /add
- 방식은 POST (사용자가 입력한 걸 숨겨서 전송해요)
- 서버에서 준 memoForm이라는 객체랑 연결할 거예요

🔵 진짜 쉬운 말:
"메모 새로 작성해서 서버에 보내줘!" 라는 역할을 하는 폼이에요
```

```html
<input th:field="*{text}">
🟡 의미:
- memoForm 안에 있는 text라는 필드와 연결돼요.
- 사용자가 여기에 메모 내용을 입력하게 돼요

🔵 진짜 쉬운 말:
"여기다가 메모 내용을 적으세요!" 라고 하는 입력칸이에요
```

```html
<button>등록</button>
🟡 의미:
- 위에 입력한 내용을 서버로 보내는 버튼이에요

🔵 진짜 쉬운 말:
"작성 완료! 등록해주세요~" 누르는 버튼이에요
```

---

```html
<p th:text="${msg}"></p>
🟡 의미:
- 서버에서 준 메시지를 보여줘요
- 예: "메모가 등록되었습니다!" 같은 알림

🔵 진짜 쉬운 말:
"방금 뭐 했는지 알려주는 공간이에요!"
```

```html
<a th:href="@{/}">홈으로</a>
🟡 의미:
- 메인 화면(/)으로 가는 링크예요

🔵 진짜 쉬운 말:
"다 썼으면 홈으로 돌아가요~" 라고 안내하는 버튼이에요
```

---

**✅ 메모 수정 페이지**

```html
    <section>
        <form th:action="@{/update/{id}(id=${memo.id()})}" th:method="post">
            <label>내용 :
                <input th:name="text" th:value="${memo.text}">
            </label>
            <button>수정</button>
        </form>
    </section>
```

**설명**

---

```html
<form th:action="@{/update/{id}(id=${memo.id()})}" th:method="post">
🟡 의미:
- 서버에 메모를 수정해서 보낼 폼이에요
- /update/3 이런 식으로 id 값을 주소에 넣어서 요청해요
- POST 방식으로 전송해요

🔵 진짜 쉬운 말:
"이 메모(id=3)를 이렇게 고쳐서 서버에 보내줄게요!" 하는 폼이에요
```

```html
<input th:name="text" th:value="${memo.text}">
🟡 의미:
- input의 name을 "text"로 하고
- 기존 메모 내용인 memo.text를 미리 넣어줄 거예요

🔵 진짜 쉬운 말:
"원래 내용이 들어가 있고, 사용자가 이걸 수정할 수 있어요"
```

```html
<button>수정</button>
🟡 의미:
- 위에서 수정한 내용을 서버에 보낼 버튼이에요

🔵 진짜 쉬운 말:
"수정 완료! 서버에 알려줘~" 하는 버튼이에요
```

---

```html
<p th:text="${msg}"></p>
🟡 의미:
- 수정 성공/실패 등 서버 메시지를 보여줘요

🔵 진짜 쉬운 말:
"수정했는지 잘 안됐는지 결과 알려주는 공간이에요"
```

```html
<a th:href="@{/}">홈으로</a>
🟡 의미:
- 다시 홈(/)으로 가는 링크예요

🔵 진짜 쉬운 말:
"수정 끝났으면 다시 메인으로 가요~" 하는 버튼이에요
```

---

**🧠 타임리프 문법 초간단 요약**

| 표현 | 쉬운 말 |
|------|----------|
| `th:action="@{/주소}"` | "이 주소로 서버에 요청 보내!" |
| `th:method="post"` | "숨겨서 보내는 방식으로 요청해!" |
| `th:object="${객체}"` | "이 폼은 이 객체랑 연결되어 있어요!" |
| `th:field="*{속성}"` | "입력칸과 서버 데이터 연결해줘!" |
| `th:name="text"` | "보낼 때 이름은 text라고 할게!" |
| `th:value="${값}"` | "이 값으로 미리 채워줄게!" |
| `th:text="${msg}"` | "이 메시지를 글로 보여줘!" |
| `th:href="@{/}"` | "이 주소로 이동해줘!" |
