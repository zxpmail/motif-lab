package com.motiflab.service;

import com.motiflab.model.QuizItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** TeachingPackGenerator 解析测试（不调真实 LLM）。 */
class TeachingPackGeneratorTest {

    @Test
    void parse_acceptsValidMotifPack() {
        TeachingPackGenerator gen = new TeachingPackGenerator(null);
        String json = """
                {
                  "title": "工厂模式",
                  "fable": "小明每天自己在后院拧螺丝做玩具车，零件摊一地，客人一换款式他就从头重装。后来街上开了柜台：客人只说要车或要船，柜台进门换模具，出来把成品交到客人手里。小明发现——自己再也不用钻进车间，只要说清楚要什么。客人变多了，他自己动手组装反而变少了。",
                  "explanation": "工厂模式就是：调用方只提需求，创建细节藏在工厂里。",
                  "contrast": [
                    {"story": "客人只说要车/船", "concept": "调用方不指定具体类"},
                    {"story": "柜台进门换模具", "concept": "工厂内部决定怎么创建"},
                    {"story": "小明不再自己组装", "concept": "创建细节被封装"}
                  ],
                  "motif": "客人要的款式变多了 → 自己动手组装反而变少了",
                  "motto": "只要说要什么，工厂自己挑做法。",
                  "quiz": [
                    {"id":"q1","question":"点单的人最不该做什么？","choices":["说清楚要什么","自己钻进车间组装","拿走成品"],"answerIndex":1},
                    {"id":"q2","question":"柜台能交车又能交船，说明什么？","choices":["点单方式要改两套","创建细节可换，点单方式可不变","只能做一种"],"answerIndex":1}
                  ]
                }
                """;
        TeachingPackGenerator.Pack pack = gen.parse("factory", json);
        assertEquals("工厂模式", pack.storyboard().title());
        assertTrue(pack.fable().length() > 80);
        assertTrue(pack.motif().contains("→"));
        assertEquals(3, pack.contrast().size());
        assertEquals(2, pack.quiz().size());
        QuizItem q1 = pack.quiz().get(0);
        assertEquals(1, q1.answerIndex());
    }

    @Test
    void parse_rejectsEmptyMotto() {
        TeachingPackGenerator gen = new TeachingPackGenerator(null);
        String json = """
                {"title":"x","fable":"这是一段足够长的占位寓言文字，用来通过长度检查，但口诀是空话。再补一些字。再补一些字。再补一些字。",
                 "explanation":"e","motif":"A 变多 → B 变少","motto":"先看懂故事，再记住这一下。",
                 "contrast":[{"story":"s","concept":"c"}],
                 "quiz":[
                  {"id":"q1","question":"好题1？","choices":["a","b","c"],"answerIndex":0},
                  {"id":"q2","question":"好题2？","choices":["a","b","c"],"answerIndex":1}
                ]}
                """;
        assertThrows(IllegalStateException.class, () -> gen.parse("x", json));
    }
}
