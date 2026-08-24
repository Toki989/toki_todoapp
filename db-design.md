# ToDo管理アプリ DB設計書

## 1. 文書の目的

本書は、`requirements.md` と `handout_app_spec.md` をもとに、ToDo管理アプリで使用するDB（データを保存・検索する仕組み）を定義する。

画面の配置や見た目は、別途作成する画面設計書で扱い、本書の対象外とする。

## 2. 設計の前提

- DBMS（DBを管理するソフトウェア）は MySQL 8.0 を使用する。
- 使用するテーブル（データを表形式で保存する場所）は `todos` の1つだけとする。
- `todos` の1行を、1件の「やること」として扱う。
- 文字コードは `utf8mb4` とし、日本語を保存できるようにする。
- `id`、`created_at`、`updated_at` は利用者が入力せず、MySQLが自動で設定する。

## 3. テーブル一覧

| テーブル名 | 日本語名 | 用途 |
|---|---|---|
| `todos` | やること | やること、メモ、ジャンル、優先度、期限、完了状態、登録日時、最終更新日時を保存する。 |

## 4. todos テーブル定義

「NULL可」は、値を入れないことを許すかどうかを表す。「既定値」は、値を指定しなかった場合に自動で入る値を表す。

| No. | カラム名 | 日本語名 | データ型（保存形式） | NULL可 | 既定値・自動設定 | 制約（保存できる値の決まり） | 対応要件 |
|---:|---|---|---|---|---|---|---|
| 1 | `id` | 識別番号 | `BIGINT` | 不可 | `AUTO_INCREMENT`（自動で1ずつ増える） | 主キー（各行を重複なく見分ける値） | DR-01、NFR-02 |
| 2 | `title` | やること | `VARCHAR(255)` | 不可 | なし | 1～255文字。空文字および半角・全角の空白だけの値は不可 | DR-02、FR-01-01、FR-02-01、FR-05-01、VR-01、VR-02 |
| 3 | `detail` | メモ | `VARCHAR(255)` | 可 | `NULL`（値なし） | 入力する場合は255文字以内 | DR-03、FR-02-01、VR-03、NFR-03 |
| 4 | `category` | ジャンル | `VARCHAR(255)` | 不可 | なし | 「デザイン」「マーケティング」「プログラミング」「資格」「就職活動」のいずれか | DR-04、FR-01-01、FR-02-01、FR-05-02、VR-04 |
| 5 | `priority` | 優先度 | `INT` | 不可 | `2`（中） | `1`（高）、`2`（中）、`3`（低）のいずれか | DR-05、FR-01-01、FR-01-03、FR-02-01、VR-05 |
| 6 | `due_date` | 期限 | `DATE` | 可 | `NULL`（値なし） | 日付。期限に関する追加の入力制限は設けない | DR-06、FR-01-01、FR-02-01、FR-06-01、FR-06-02 |
| 7 | `completed` | 完了状態 | `BOOLEAN` | 不可 | `FALSE`（未完了） | `FALSE` / `0`（未完了）、`TRUE` / `1`（完了）のいずれか | DR-07、FR-01-01、FR-01-04、FR-03-01 |
| 8 | `created_at` | 登録日時 | `DATETIME` | 不可 | 登録時の日時を自動設定 | 利用者は入力しない | DR-08 |
| 9 | `updated_at` | 最終更新日時 | `DATETIME` | 不可 | 登録時および更新時の日時を自動設定 | 利用者は入力しない | DR-09 |

## 5. キー・制約・インデックス

### 5.1 主キー

`id` を主キー（各行を重複なく見分ける値）とする。MySQLの `AUTO_INCREMENT` により、登録のたびに番号を自動で増やす。

### 5.2 値の制約

- `title` は必須とし、空文字や半角・全角の空白だけの値を保存できないようにする。
- `category` は、要件で指定された5種類以外を保存できないようにする。
- `priority` は、`1`、`2`、`3` 以外を保存できないようにする。
- `completed` は、`0` または `1` 以外を保存できないようにする。
- 画面側でも同じ入力チェックを行い、利用者に要件で指定されたエラーメッセージを表示する。DBの制約は、不正なデータの保存を最後に防ぐためのものとする。

### 5.3 インデックス

`due_date` に `idx_todos_due_date` というインデックス（検索や並び替えを速くする目印）を設定する。期限順の並び替え（FR-06-01、FR-06-02）で利用する。

### 5.4 期限順の並び替え

`due_date` が `NULL`（期限未入力）のデータは、期限が近い順と遠い順のどちらでも末尾に並べる。

## 6. 要件とカラムの対応

| 要件ID | 要件の要点 | 対応カラム | DB設計での対応 |
|---|---|---|---|
| DR-01 | 識別番号 | `id` | 主キー、自動採番（番号の自動割り当て）、必須 |
| DR-02 | やること | `title` | `VARCHAR(255)`、必須、空白だけは不可 |
| DR-03 | メモ | `detail` | `VARCHAR(255)`、任意 |
| DR-04 | ジャンル | `category` | 必須、5種類に制限 |
| DR-05 | 優先度 | `priority` | 必須、`1`～`3` に制限、既定値 `2` |
| DR-06 | 期限 | `due_date` | 任意の日付 |
| DR-07 | 完了状態 | `completed` | 必須、`0` / `1` に制限、既定値 `0` |
| DR-08 | 登録日時 | `created_at` | 登録時に自動設定 |
| DR-09 | 最終更新日時 | `updated_at` | 登録時と更新時に自動設定 |
| FR-05-01 | 名前の一部一致検索 | `title` | `title` を検索対象にする |
| FR-05-02 | ジャンルの完全一致検索 | `category` | `category` を検索対象にする |
| FR-06-01、FR-06-02、FR-06-05 | 期限順の並び替え | `due_date` | `due_date` を並び替えの基準にし、期限が未入力のデータは昇順・降順のどちらでも末尾にする。インデックスを設定する |
| NFR-02 | 操作ログに対象IDを記録 | `id` | 登録・編集・削除時に対象の `id` をログへ渡せる |
| NFR-03 | ログに本文を記録しない | `title`、`detail` | ログ出力では、この2カラムの値を使用しない |

## 7. CREATE TABLE 文（DDL）

DDLは、テーブルの形を作るためのSQL文（DBへの命令文）である。次のSQLを実行して `todos` テーブルを作成する。

```sql
CREATE TABLE todos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    detail VARCHAR(255) NULL,
    category VARCHAR(255) NOT NULL,
    priority INT NOT NULL DEFAULT 2,
    due_date DATE NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT chk_todos_title_not_blank
        CHECK (CHAR_LENGTH(TRIM(REPLACE(title, '　', ' '))) > 0),
    CONSTRAINT chk_todos_category
        CHECK (category IN (
            'デザイン',
            'マーケティング',
            'プログラミング',
            '資格',
            '就職活動'
        )),
    CONSTRAINT chk_todos_priority
        CHECK (priority IN (1, 2, 3)),
    CONSTRAINT chk_todos_completed
        CHECK (completed IN (0, 1)),

    INDEX idx_todos_due_date (due_date)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

## 8. 補足

- `BOOLEAN` は、MySQLでは実際には `0` または `1` を保存する形式として扱われる。
- `CURRENT_TIMESTAMP` は、SQL実行時の現在日時を自動で入れる指定である。
- `CHECK` は、決められた値以外を保存しないための検査である。本書のDDLは、`CHECK` が有効な MySQL 8.0.16 以降を前提とする。
- `title` と `detail` の255文字制限は、DBと画面側の両方で確認する。
