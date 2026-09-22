## Content Review and Approval

When generating multiple questions, do not immediately generate the final JSON content.

The generation process should be split into two stages:

### Stage 1 — Question Proposal

First, generate a human-readable list of proposed questions.

At this stage:

- Show the questions and their answers in a readable format.
- Make it easy for the user to review and modify the proposed content.
- Do not generate the final JSON yet.
- The user may request changes, such as:
    - replacing a question;
    - changing an answer;
    - changing the correct answer;
    - changing the difficulty;
    - changing the question type;
    - removing a question;
    - adding new questions;
    - changing the distribution of question types or difficulty.

Continue refining the proposed list until the user explicitly approves it.

### Stage 2 — JSON Generation

Only after the user explicitly approves the proposed list:

- Generate the final JSON according to the specified question structure.
- Include all approved questions.
- Apply all requested changes from the review process.
- Do not introduce new questions or modify approved content unless explicitly requested.
- Ensure that the resulting JSON follows all technical requirements defined in this document.

The final JSON should contain only the questions that were approved by the user.

# Content Generation Guidelines

These guidelines define the default rules for generating quiz content.

Follow these guidelines unless the user explicitly requests something different.

Explicit requirements from the user's request take priority over these guidelines.
If the user intentionally asks to violate a recommendation, follow the user's request as long as it does not violate the JSON structure or technical requirements of the requested question type.

## Generation Workflow

The recommended workflow is:

1. The user provides the topic and generation requirements.
2. Generate a proposed list of questions in a human-readable format.
3. The user reviews and modifies the proposed questions.
4. Repeat the review process until the user approves the list.
5. After approval, generate the final JSON content.
6. The user can save the JSON to a file and import it into Cleverest.

## Language

The language of the generated quiz content is determined by the user's request, not by the language of this instruction document.
The instructions in this document are written in English for consistency, but questions, answers, explanations, hints, categories, and other user-facing content may be generated in any language requested by the user.
If the user requests Russian content, generate all quiz content in Russian.
Do not translate the quiz content into English unless explicitly requested.

## Question Quality

- Questions should be clear, concise, and unambiguous.
- A question should have a well-defined correct answer.
- Avoid questions that depend on subjective opinions unless the user explicitly requests opinion-based questions.
- Avoid trick questions unless the user explicitly requests them.
- The question should contain enough information to determine the correct answer.
- Do not make a question unnecessarily complicated just to increase its difficulty.

## Factual Accuracy

- Use established and verifiable facts.
- Do not invent facts, dates, names, statistics, quotations, technical specifications, or other information.
- Do not use uncertain information as the basis for a question.
- Avoid controversial or disputed claims unless the user explicitly asks for them.
- When a fact has changed over time, make the relevant timeframe clear in the question.

## Answer Quality

- Incorrect answers should be plausible and relevant to the question.
- Avoid obviously absurd or unrelated incorrect answers.
- Avoid giving grammatical or structural clues that make the correct answer obvious.
- Avoid making the correct answer noticeably longer or more detailed than the incorrect answers without a good reason.
- Do not use duplicate answers.
- The correct answer must actually answer the question.

## Difficulty

### Easy

Easy questions should generally:
- cover common knowledge or fundamental concepts;
- require little specialized knowledge;
- have a relatively straightforward answer.

### Medium

Medium questions should generally:
- require good knowledge of the requested topic;
- require recalling a specific fact or concept;
- use plausible alternatives that require some knowledge to distinguish.

### Hard

Hard questions should generally:
- require specialized or detailed knowledge;
- involve less commonly known facts;
- require connecting or distinguishing between related facts or concepts.

Difficulty should be appropriate for the requested topic.

Do not make a question artificially difficult by using obscure wording or unnecessary complexity.

## Diversity

When generating multiple questions:

- Avoid asking essentially the same question multiple times.
- Avoid repeatedly testing the same fact.
- Cover different aspects of the requested topic when possible.
- Vary question formulations and subject areas.
- When the user requests a large number of questions, distribute them across relevant subtopics instead of focusing on a single narrow aspect.

## User Requirements

Always follow explicit requirements from the user regarding:

- number of questions;
- difficulty;
- topic and subtopics;
- requested question types;
- number of answers or pairs;
- requested use of images;
- requested distribution of question types;
- any other explicitly specified content requirements.

If the user provides a specific distribution or constraint, do not replace it with the default recommendations above.

# Question structure

Every question has a common set of fields, regardless of its question type.

### Question fields

| Field | Type | Required | Description                                                                                                 | Example |
|---|---|---|-------------------------------------------------------------------------------------------------------------|---|
| `type` | string | yes | Import type of the question. Must be one of the supported import types described below.                     | `"FOUR_ANSWERS"` |
| `text` | string | yes | The question text.                                                                                          | `"What is the capital of France?"` |
| `category` | string | yes | Name of an existing question category.                                                                      | `"Geography"` |
| `photoLocation` | string | no | URL of an image associated with the question. The application downloads and stores the image automatically. | `"https://example.com/image.png"` |
| `answerDescriptionText` | string | no | Explanation shown after answering the question.                                                             | `"Paris is the capital of France."` |
| `hintsText` | string | no | One or more hints. Each hint must be placed on a separate line (\n)                                         | `"It is located in Europe.\nIt is also the largest city in France."` |

## Answer structure

Answers are defined as part of the question and their exact structure depends on the question type.

The common answer fields are:

| Field     | Type    | Required                            | Description                              | Example   |
|-----------|---------|-------------------------------------|------------------------------------------|-----------|
| `text`    | string  | mostly required                     | Text of the answer.                      | `"Paris"` |
| `correct` | boolean | mostly required                     | Indicates whether the answer is correct. | `true`    |
| `index`   | int     | optional (depends on question type) | Indicates answer order.                  | `1`       |

## Base JSON structure

Every imported question follows this basic structure:

```json
{
  "type": "IMPORT_TYPE",
  "text": "Question text",
  "category": "Category name",
  "photoLocation": "https://example.com/image.png",
  "answerDescriptionText": "Explanation of the answer.",
  "hintsText": "First hint.\nSecond hint.",
  "answers": [
    {
      "text": "Answer 1",
      "correct": false
    },
    {
      "text": "Answer 2",
      "correct": true
    }
  ]
}
```

## Question types

The `type` field must contain exactly one of the supported import types defined in this document.

Do not invent new values for `type`.

The `type` value is part of the import contract and must not be translated.

## Question type: TEXT

### Description

`TEXT` is a standard multiple-choice question with exactly four answer options.

The question must have exactly one correct answer.

All four answer options must contain text.

### Keywords

Use `TEXT` when the user asks for:

- a standard question
- a classic question
- a normal question
- a multiple-choice question with one correct answer
- a question with four answer options
- a question where the player chooses one correct answer

If the user does not explicitly request another question type and asks for a regular question, use `TEXT`.

### JSON structure

The following fields are specific to `TEXT`:

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `answers` | array | yes | Exactly four answer options. | `[...]` |
| `answers[].text` | string | yes | Text of the answer option. | `"Paris"` |
| `answers[].correct` | boolean | yes | Indicates whether this answer is correct. Exactly one answer must be `true`. | `true` |

The answer options must be provided in the desired display order.

### Example

```json
{
  "type": "TEXT",
  "text": "What is the capital of France?",
  "category": "Geography",
  "answerDescriptionText": "Paris is the capital and largest city of France.",
  "hintsText": "It is located in northern France.\nIt is one of Europe's major cities.",
  "answers": [
    {
      "text": "Paris",
      "correct": true
    },
    {
      "text": "London",
      "correct": false
    },
    {
      "text": "Berlin",
      "correct": false
    },
    {
      "text": "Madrid",
      "correct": false
    }
  ]
}
```

## Question type: MULTI

### Description

`MULTI` uses the same structure and rules as `TEXT`.

The only difference is that a `MULTI` question can have multiple correct answers.

### Keywords

Use `MULTI` when the user asks for:

- a multiple-answer question
- a question with multiple correct answers
- a question with several correct answers
- a question where multiple options can be selected
- a multi-select question
- multiple valid answers
- several correct options

### JSON structure

Use the same JSON structure as described in the `TEXT` question type.

The `answers` array must contain exactly four answer options.

The `correct` field determines whether an answer is correct.

Unlike `TEXT`, multiple answers may have `correct: true`.

### Example

```json
{
  "type": "MULTI",
  "text": "Which of these countries are members of the European Union?",
  "category": "Geography",
  "answerDescriptionText": "France, Germany and Poland are members of the European Union.",
  "answers": [
    {
      "text": "France",
      "correct": true
    },
    {
      "text": "Germany",
      "correct": true
    },
    {
      "text": "United Kingdom",
      "correct": false
    },
    {
      "text": "Poland",
      "correct": true
    }
  ]
}
```

## Question type: PRECISION

### Description

`PRECISION` is a question where the player must provide a numerical answer.

The question has exactly one correct numerical answer and an allowed range of deviation.

The main question fields and general rules are the same as described in the common question structure.

### Keywords

Use `PRECISION` when the user asks for:

- a precision question
- an exact answer question
- a question requiring a number
- a question requiring a numerical answer
- a question where the answer is a number
- a question where the answer is a year
- a question where the answer is a date expressed as a number
- a question where the player must guess a quantity
- a question where the player must enter an exact value
- "how many"
- "how much"
- "what year"
- "in which year"

### JSON structure

In addition to the common question fields, `PRECISION` requires the following fields:

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `answerText` | number | yes | The correct numerical answer. | `1969` |
| `range` | number | yes | The allowed deviation from the correct answer. | `2` |

`PRECISION` does not use the `answers` array.

The correct answer is defined by `answerText`.

The `range` determines how far the player's answer may differ from `answerText` while still being considered correct.

### Example

```json
{
  "type": "PRECISION",
  "text": "In which year did humans first land on the Moon?",
  "category": "Space",
  "answerText": 1969,
  "range": 0,
  "answerDescriptionText": "Apollo 11 landed on the Moon in 1969.",
  "hintsText": "The mission was launched in the 1960s.\nThe mission was called Apollo 11."
}
```

## Question type: OR

### Description

`OR` is a question where the player chooses between two possible answers.

The question has exactly two answer options:

- one correct answer;
- one incorrect answer.

The correct and incorrect answers are represented by separate fields rather than an `answers` array.

### Keywords

Use `OR` when the user asks for:

- an either-or question
- an either/or question
- a two-option question
- a question with two possible answers
- a question where the player chooses between two options
- "A or B"
- "this or that"

### JSON structure

In addition to the common question fields, `OR` requires the following fields:

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `correctAnswerText` | string | yes | The correct answer option. | `"Yes"` |
| `optionAnswerText` | string | yes | The incorrect answer option. | `"No"` |

`OR` does not use the `answers` array.

Do not provide `correct` or `index` fields.

Exactly one of the two options must be the correct answer, represented by `correctAnswerText`.

The two answer options must be clearly distinct and must directly answer the question.

### Example

```json
{
  "type": "OR",
  "text": "Is the Earth larger than the Moon?",
  "category": "Space",
  "correctAnswerText": "Yes",
  "optionAnswerText": "No",
  "answerDescriptionText": "The Earth is significantly larger than the Moon."
}
```

## Question type: LINK

### Description

`LINK` is a matching question where the player needs to match items from the left side with the corresponding items from the right side.

The question contains the same common fields as other question types.

Instead of the `answers` array, `LINK` uses two groups of answers:

- `leftAnswers`
- `rightAnswers`

Each item must be placed on a separate line.

The two groups must contain the same number of items.

### Keywords

Use `LINK` when the user asks for:

- a matching question
- a match question
- a matching pairs question
- a question about matching items
- match the following
- match A with B
- connect the pairs
- pair the items
- сопоставить
- сопоставление
- соединить пары
- найти соответствия
- match

### JSON structure

In addition to the common question fields, `LINK` requires the following fields:

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `leftAnswers` | string | yes | Items on the left side. Each item must be on a separate line. | `"France\nGermany\nItaly"` |
| `rightAnswers` | string | yes | Items on the right side. Each item must be on a separate line. | `"Rome\nBerlin\nParis"` |

### Important rules

- `leftAnswers` and `rightAnswers` must contain the same number of items.
- Each item must be separated by a newline.
- Generate at least 3 pairs.
- Prefer 3 to 5 pairs for normal questions.
- 6 pairs may be used when the topic requires more items.
- Every item on the left must have exactly one corresponding item on the right.
- Every item on the right must have exactly one corresponding item on the left.
- Do not use the `answers` array.
- Do not generate `correct` or `index` fields.
- The order of items on the left and right sides does not need to correspond. The application will use the underlying item values to determine the matching pairs.

### Example

```json
{
  "type": "LINK",
  "text": "Match each country with its capital.",
  "category": "Geography",
  "answerDescriptionText": "Paris is the capital of France, Berlin is the capital of Germany, and Rome is the capital of Italy.",
  "hintsText": "All countries are European.",
  "leftAnswers": "France\nGermany\nItaly",
  "rightAnswers": "Rome\nBerlin\nParis"
}
```

## Question type: TOP

### Description

`TOP` is a question where the player must arrange a list of items according to the criteria specified in the question.

The question contains a list of text items.

The list does not have to follow a chronological or naturally ordered sequence. The question itself defines how the items should be ranked or arranged.

### Keywords

Use `TOP` when the user asks for:

- a top list
- a ranking
- rank these items
- rank from best to worst
- rank from largest to smallest
- order these items
- arrange these items
- create a top
- рейтинг
- топ
- расставить по местам
- упорядочить
- расположить по рейтингу

### JSON structure

In addition to the common question fields, `TOP` requires:

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `topListText` | string | yes | List of items. Each item must be placed on a separate line. | `"Item A\nItem B\nItem C"` |

Do not use the `answers` array.

Do not provide an `index` field. The application assigns indexes based on the order of items in `topListText`.

### Example

```json
{
  "type": "TOP",
  "sequence": false,
  "text": "Rank these planets from largest to smallest.",
  "category": "Space",
  "topListText": "Jupiter\nSaturn\nUranus\nNeptune"
}
```

## Question type: SEQUENCE

### Description

`SEQUENCE` uses the same structure as `TOP`.

The difference is that `SEQUENCE` requires the items to be arranged according to an objectively correct sequence.

The sequence may be based on:

- chronology
- stages of a process
- progression
- size
- increasing or decreasing values
- any other clearly defined ordering criterion

### Keywords

Use `SEQUENCE` when the user asks for:

- put in chronological order
- arrange in chronological order
- order by date
- order by year
- arrange in sequence
- put in the correct order
- order of events
- sequence of events
- chronological order
- chronology
- timeline
- steps
- stages
- порядок
- хронология
- последовательность
- расставить по порядку
- расположить в правильном порядке

### JSON structure

Use the same JSON structure as described for `TOP`.

Additional field:

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `sequence` | boolean | yes | Determines whether the question is a `TOP` or `SEQUENCE` question. | `true` |

Set `sequence` to:

- `false` for a `TOP` question.
- `true` for a `SEQUENCE` question.

The items in `topListText` must be provided in the correct order.

### Example

```json
{
  "type": "SEQUENCE",
  "sequence": true,
  "text": "Put these events in chronological order.",
  "category": "History",
  "topListText": "French Revolution\nAmerican Civil War\nWorld War I\nWorld War II"
}
```

## Question type: PHOTO

### Description

`PHOTO` is a multiple-choice question where the answer options are represented by images instead of text.

The question must have exactly four answer options.

Each answer option must be a URL pointing to an image.

Exactly one of the four images must be the correct answer.

### Keywords

Use `PHOTO` when the user asks for:

- a photo question
- a picture question
- a question with photo answers
- a question with image answers
- a visual question
- a question where the player chooses an image
- фото вопрос
- вопрос с фотографиями
- вопрос с картинками
- выбрать правильную фотографию
- выбрать правильное изображение

### JSON structure

In addition to the common question fields, `PHOTO` requires the following fields:

| Field | Type | Required | Description | Example |
|---|---|---|---|---|
| `correctOption` | string | yes | URL of the image representing the correct answer. | `"https://example.com/paris.jpg"` |
| `optionTwo` | string | yes | URL of the second answer image. | `"https://example.com/london.jpg"` |
| `optionThree` | string | yes | URL of the third answer image. | `"https://example.com/berlin.jpg"` |
| `optionFour` | string | yes | URL of the fourth answer image. | `"https://example.com/madrid.jpg"` |

All four fields must contain valid image URLs.

`PHOTO` does not use the `answers` array.

Do not provide `correct` or `index` fields.

The `correctOption` field must contain the URL of the correct image.

The three other fields must contain incorrect image options.

The four images should be visually suitable for comparison and should directly correspond to the question.

### Example

```json
{
  "type": "PHOTO",
  "text": "Which of these cities is Paris?",
  "category": "Geography",
  "correctOption": "https://example.com/paris.jpg",
  "optionTwo": "https://example.com/london.jpg",
  "optionThree": "https://example.com/berlin.jpg",
  "optionFour": "https://example.com/madrid.jpg",
  "answerDescriptionText": "Paris is the capital of France."
}
```