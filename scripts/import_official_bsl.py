from __future__ import annotations

from dataclasses import dataclass
from io import BytesIO
from pathlib import Path
import re

from minio import Minio
from PIL import Image
import pymysql


SOURCE_ROOT = Path(r"C:\Users\77349\sign-assets\raw\BSLExamples\BSLExamples")
SIGNFILES_DIR = SOURCE_ROOT / "SignFiles"
HTML_DIR = SOURCE_ROOT
BUCKET_NAME = "cwasa-static"
MINIO_ENDPOINT = "127.0.0.1:9000"
MINIO_ACCESS_KEY = "admin"
MINIO_SECRET_KEY = "admin123456"

MYSQL_CONFIG = {
    "host": "127.0.0.1",
    "port": 3308,
    "user": "root",
    "password": "123456",
    "database": "hearbridge",
    "charset": "utf8mb4",
    "autocommit": False,
}

HTML_ITEM_PATTERN = re.compile(
    r'startPlayer\("SignFiles/([^"]+\.sigml)"\).*?<img src="([^"]+)"',
    re.S,
)


@dataclass(frozen=True)
class CategoryDef:
    code: str
    name_zh: str
    html_file: str | None = None


@dataclass(frozen=True)
class ResourceRecord:
    code: str
    name_zh: str
    category_code: str
    sigml_path: Path
    image_path: Path

    @property
    def sigml_object_key(self) -> str:
        return f"sigml/official-bsl/{self.code}.sigml"

    @property
    def cover_object_key(self) -> str:
        return f"images/sign/official-bsl/{self.code}.png"


CATEGORY_DEFS = [
    CategoryDef("alphabet", "字母", "AlphabetIndex.html"),
    CategoryDef("colour", "颜色", "ColourIndex.html"),
    CategoryDef("country", "国家地区", "CountryIndex.html"),
    CategoryDef("describing", "描述表达", "DescribingIndex.html"),
    CategoryDef("number", "数字", "NumberIndex.html"),
    CategoryDef("people", "人物称谓", "PeopleIndex.html"),
    CategoryDef("phrase", "常用短语", "PhrasesIndex.html"),
    CategoryDef("question", "疑问表达", "QuestionIndex.html"),
    CategoryDef("time", "时间", "TimeIndex.html"),
    CategoryDef("object", "物品"),
    CategoryDef("weather", "天气"),
]

LETTER_NAME_ZH = {
    "A": "字母A",
    "B": "字母B",
    "C": "字母C",
    "D": "字母D",
    "E": "字母E",
    "F": "字母F",
    "G": "字母G",
    "H": "字母H",
    "Iv": "字母I",
    "J": "字母J",
    "K": "字母K",
    "L": "字母L",
    "M": "字母M",
    "N": "字母N",
    "O": "字母O",
    "P": "字母P",
    "Q": "字母Q",
    "R": "字母R",
    "S": "字母S",
    "T": "字母T",
    "U": "字母U",
    "V": "字母V",
    "W": "字母W",
    "X": "字母X",
    "Y": "字母Y",
    "Z": "字母Z",
}

NUMBER_NAME_ZH = {
    "0": "零",
    "1": "一",
    "2": "二",
    "3": "三",
    "4": "四",
    "5": "五",
    "6": "六",
    "7": "七",
    "8": "八",
    "9": "九",
    "10": "十",
    "11": "十一",
    "12": "十二",
    "13": "十三",
    "100": "一百",
    "hundred": "百",
}

RESOURCE_NAME_ZH = {
    "colour": "颜色",
    "red": "红色",
    "blue": "蓝色",
    "green": "绿色",
    "yellow": "黄色",
    "purple": "紫色",
    "orange": "橙色",
    "black": "黑色",
    "brown": "棕色",
    "grey": "灰色",
    "gold": "金色",
    "silver": "银色",
    "white": "白色",
    "pink": "粉色",
    "cream": "米色",
    "england": "英格兰",
    "wales": "威尔士",
    "scotland": "苏格兰",
    "britain": "英国",
    "france": "法国",
    "germany": "德国",
    "austria": "奥地利",
    "switzerland": "瑞士",
    "holland": "荷兰",
    "australia": "澳大利亚",
    "america": "美国",
    "belgium": "比利时",
    "greece": "希腊",
    "asia": "亚洲",
    "africa": "非洲",
    "good": "好",
    "better": "更好",
    "best": "最好",
    "nice": "不错",
    "beautiful": "美丽",
    "bad": "坏",
    "worse": "更糟",
    "worst": "最糟",
    "ugly": "丑",
    "awful": "糟糕",
    "same": "相同",
    "different": "不同",
    "easy": "容易",
    "difficult": "困难",
    "many": "多",
    "quick": "快",
    "slow": "慢",
    "near": "近",
    "far": "远",
    "few": "少",
    "quiet": "安静",
    "loud": "大声",
    "new": "新",
    "old": "旧",
    "soft": "软",
    "I": "我",
    "my": "我的",
    "you": "你",
    "your": "你的",
    "we": "我们",
    "married": "结婚",
    "boy": "男孩",
    "girl": "女孩",
    "man": "男人",
    "woman": "女人",
    "father": "父亲",
    "mother": "母亲",
    "hello": "你好",
    "bye": "再见",
    "yes": "是",
    "no": "不",
    "yournamewhat": "你叫什么名字",
    "howareyou": "你好吗",
    "youhowold": "你几岁了",
    "livewhere": "你住在哪里",
    "yourhobbieswhat": "你的爱好是什么",
    "anothertime2": "改天",
    "iunderstand": "我明白了",
    "donotunderstand": "我不明白",
    "bsl": "英国手语",
    "canyousign": "你会打手语吗",
    "anothertime": "另一个时间",
    "what": "什么",
    "why": "为什么",
    "when": "什么时候",
    "where": "哪里",
    "who": "谁",
    "which": "哪一个",
    "how": "怎么",
    "namewhat": "叫什么名字",
    "time": "时间",
    "morning": "早上",
    "afternoon": "下午",
    "evening": "晚上",
    "everyday": "每天",
    "yesterday": "昨天",
    "today": "今天",
    "tomorrow": "明天",
    "every": "每个",
    "Computer": "电脑",
    "Rain": "下雨",
    "Table.AUS": "桌子",
}

FALLBACK_MAPPING = {
    "every": ("time", SOURCE_ROOT / "Images" / "Time" / "Every.jpg"),
    "hundred": ("number", SOURCE_ROOT / "Images" / "Numbers" / "100.jpg"),
    "anothertime": ("phrase", SOURCE_ROOT / "Images" / "descriptions" / "another.jpg"),
    "namewhat": ("question", SOURCE_ROOT / "Images" / "Questions" / "What.jpg"),
    "Computer": ("object", SOURCE_ROOT / "Images" / "blank.jpg"),
    "Rain": ("weather", SOURCE_ROOT / "Images" / "blank.jpg"),
    "Table.AUS": ("object", SOURCE_ROOT / "Images" / "blank.jpg"),
}


def resolve_name_zh(code: str) -> str:
    for mapping in (LETTER_NAME_ZH, NUMBER_NAME_ZH, RESOURCE_NAME_ZH):
        if code in mapping:
            return mapping[code]
    raise KeyError(f"Missing Chinese name mapping for code: {code}")


def parse_index_html(html_path: Path, category_code: str) -> list[ResourceRecord]:
    html = html_path.read_text(encoding="utf-8", errors="ignore")
    records: list[ResourceRecord] = []
    for sigml_name, image_rel_path in HTML_ITEM_PATTERN.findall(html):
        code = Path(sigml_name).stem
        sigml_path = SIGNFILES_DIR / sigml_name
        image_path = SOURCE_ROOT / Path(image_rel_path)
        if not sigml_path.exists():
            raise FileNotFoundError(f"Missing sigml file: {sigml_path}")
        if not image_path.exists():
            raise FileNotFoundError(f"Missing image file: {image_path}")
        records.append(
            ResourceRecord(
                code=code,
                name_zh=resolve_name_zh(code),
                category_code=category_code,
                sigml_path=sigml_path,
                image_path=image_path,
            )
        )
    return records


def build_records() -> tuple[list[CategoryDef], list[ResourceRecord]]:
    resources_by_code: dict[str, ResourceRecord] = {}

    for category in CATEGORY_DEFS:
        if not category.html_file:
            continue
        html_path = HTML_DIR / category.html_file
        for record in parse_index_html(html_path, category.code):
            if record.code in resources_by_code:
                raise ValueError(f"Duplicate resource code detected: {record.code}")
            resources_by_code[record.code] = record

    all_sigml_codes = {
        sigml_path.stem: sigml_path for sigml_path in SIGNFILES_DIR.glob("*.sigml")
    }
    missing_codes = sorted(set(all_sigml_codes) - set(resources_by_code))
    for code in missing_codes:
        if code not in FALLBACK_MAPPING:
            raise ValueError(f"Missing fallback mapping for code: {code}")
        category_code, image_path = FALLBACK_MAPPING[code]
        if not image_path.exists():
            raise FileNotFoundError(f"Missing fallback image file: {image_path}")
        resources_by_code[code] = ResourceRecord(
            code=code,
            name_zh=resolve_name_zh(code),
            category_code=category_code,
            sigml_path=all_sigml_codes[code],
            image_path=image_path,
        )

    used_categories = {record.category_code for record in resources_by_code.values()}
    categories = [category for category in CATEGORY_DEFS if category.code in used_categories]
    resources = [resources_by_code[code] for code in sorted(resources_by_code)]
    return categories, resources


def upload_resources(client: Minio, resources: list[ResourceRecord]) -> None:
    for index, record in enumerate(resources, start=1):
        sigml_bytes = record.sigml_path.read_bytes()
        client.put_object(
            BUCKET_NAME,
            record.sigml_object_key,
            BytesIO(sigml_bytes),
            length=len(sigml_bytes),
            content_type="application/xml",
        )

        buffer = BytesIO()
        with Image.open(record.image_path) as image:
            image.convert("RGBA").save(buffer, format="PNG")
        png_bytes = buffer.getvalue()
        client.put_object(
            BUCKET_NAME,
            record.cover_object_key,
            BytesIO(png_bytes),
            length=len(png_bytes),
            content_type="image/png",
        )

        if index % 25 == 0 or index == len(resources):
            print(f"Uploaded {index}/{len(resources)} resources")


def replace_database_rows(categories: list[CategoryDef], resources: list[ResourceRecord]) -> None:
    connection = pymysql.connect(**MYSQL_CONFIG)
    try:
        with connection.cursor() as cursor:
            cursor.execute("DELETE FROM sign_resource")
            cursor.execute("DELETE FROM sign_category")

            category_rows = [(category.code, category.name_zh, None) for category in categories]
            cursor.executemany(
                """
                INSERT INTO sign_category (code, name_zh, cover_object_key)
                VALUES (%s, %s, %s)
                """,
                category_rows,
            )

            resource_rows = [
                (
                    record.code,
                    record.name_zh,
                    record.category_code,
                    record.sigml_object_key,
                    record.cover_object_key,
                )
                for record in resources
            ]
            cursor.executemany(
                """
                INSERT INTO sign_resource (
                    code,
                    name_zh,
                    category_code,
                    sigml_object_key,
                    cover_object_key
                ) VALUES (%s, %s, %s, %s, %s)
                """,
                resource_rows,
            )
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


def main() -> None:
    categories, resources = build_records()

    print(f"Categories: {len(categories)}")
    for category in categories:
        count = sum(1 for resource in resources if resource.category_code == category.code)
        print(f"  {category.code}: {count}")

    print(f"Resources: {len(resources)}")

    client = Minio(
        MINIO_ENDPOINT,
        access_key=MINIO_ACCESS_KEY,
        secret_key=MINIO_SECRET_KEY,
        secure=False,
    )
    upload_resources(client, resources)
    replace_database_rows(categories, resources)
    print("Import completed.")


if __name__ == "__main__":
    main()
