package com.motiflab.service;

import com.motiflab.model.QuizItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** TeachingPackGenerator 解析测试（不调真实 LLM）。 */
class TeachingPackGeneratorTest {

    @Test
    void parse_acceptsValidPack() {
        TeachingPackGenerator gen = new TeachingPackGenerator(null);
        String json = """
                {
                  "title": "工厂模式",
                  "motto": "只要说要什么，工厂自己挑做法，你不用自己组装。",
                  "beats": [
                    {"who":"小明","action":"自己拧螺丝做小车","result":"又慢又容易装错","principle":"调用方自己创建很累"},
                    {"who":"小明","action":"去柜台说只要小车","result":"柜台进去换模具","principle":"创建细节藏在工厂里"},
                    {"who":"柜台","action":"交出做好的小车","result":"小明不用碰零件","principle":"只要产品，不要组装过程"},
                    {"who":"旁白","action":"再要一艘小船","result":"同一柜台换模具再交船","principle":"换种类也不改点单方式"}
                  ],
                  "quiz": [
                    {"id":"q1","question":"工厂模式里，点单的人最不该做什么？","choices":["说清楚要什么","自己钻进车间组装","拿走成品"],"answerIndex":1},
                    {"id":"q2","question":"柜台能交出车又能交船，说明什么？","choices":["点单方式要改两套","创建细节可换，点单方式可不变","只能做一种东西"],"answerIndex":1}
                  ]
                }
                """;
        TeachingPackGenerator.Pack pack = gen.parse("factory", json);
        assertEquals("工厂模式", pack.storyboard().title());
        assertTrue(pack.motto().contains("工厂"));
        assertEquals(4, pack.storyboard().beats().size());
        assertEquals(2, pack.quiz().size());
        QuizItem q1 = pack.quiz().get(0);
        assertEquals(1, q1.answerIndex());
    }

    @Test
    void parse_rejectsEmptyMotto() {
        TeachingPackGenerator gen = new TeachingPackGenerator(null);
        String json = """
                {"title":"x","motto":"先看懂故事，再记住这一下。","beats":[
                  {"who":"a","action":"b","result":"c","principle":"d"},
                  {"who":"a","action":"b","result":"c","principle":"d"},
                  {"who":"a","action":"b","result":"c","principle":"d"}
                ],"quiz":[
                  {"id":"q1","question":"好题1？","choices":["a","b","c"],"answerIndex":0},
                  {"id":"q2","question":"好题2？","choices":["a","b","c"],"answerIndex":1}
                ]}
                """;
        assertThrows(IllegalStateException.class, () -> gen.parse("x", json));
    }
}
