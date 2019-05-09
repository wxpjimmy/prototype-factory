package com.jimmy.prototype.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class StockJacksonTest {
    static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    public static void  main(String[] args) throws ParseException, IOException {
        Date date = format.parse("2018-09-03");
        Stock stock = new Stock("xiaomi", 10.6, date, "xiaomi coporation");

        ObjectMapper mapper = new ObjectMapper();
        String content = mapper.writeValueAsString(stock);
        System.out.println(content);

        mapper.setDateFormat(format);
        content = mapper.writeValueAsString(stock);
        System.out.println(content);

        String deserializedContent = "{\"name\":\"xiaomi\",\"price\":10.6,\"date\":\"2018-09-03\"}";
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Stock ds = mapper.readValue(deserializedContent, Stock.class);
        //在序列化时忽略值为 null 的属性
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //忽略值为默认值的属性
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
        System.out.println(ds);

        ListOrMapTest(mapper);
    }

    public static void ListOrMapTest(ObjectMapper mapper) throws ParseException, IOException {
        //list
        System.out.println("==========================LIST======================");
        List<Stock> testListData = new ArrayList<>();
        testListData.add(new Stock("sina", 50.3, format.parse("2019-04-08"), "sina coporation"));
        testListData.add(new Stock("apple", 200.3, format.parse("2019-04-08"), "apple coporation"));
        testListData.add(new Stock("microsoft", 120.5, format.parse("2019-04-08"), "microsoft coporation"));
        testListData.add(new Stock("google", 1210.3, format.parse("2019-04-08"), "google coporation"));

        String content = mapper.writeValueAsString(testListData);
        System.out.println(content);
        CollectionType javaType = mapper.getTypeFactory()
                .constructCollectionType(List.class, Stock.class);

        List<Stock> dl = mapper.readValue(content, javaType);

        List<Stock> d2 = mapper.readValue(content, new TypeReference<List<Stock>>(){});
        System.out.println(dl.size() + ", " + d2.size());
        System.out.println(dl.containsAll(d2));

        //map
        System.out.println("==========================MAP======================");
        Map<String, Stock> dm = new HashMap<>();
        testListData.stream().forEach(p->dm.put(p.getName(), p));
        content = mapper.writeValueAsString(dm);
        System.out.println(content);

        MapType mType = mapper.getTypeFactory()
                .constructMapType(HashMap.class, String.class, Stock.class);
        Map<String, Stock> dm1 = mapper.readValue(content, mType);
        Map<String, Stock> dm2 = mapper.readValue(content, new TypeReference<Map<String, Stock>>(){});

        System.out.println(dm1.size() + ", " + dm2.size());
        boolean equals = true;
        for(String name: dm1.keySet()) {
            if(!dm2.containsKey(name)) {
                equals = false;
                break;
            }
        }
        System.out.println(equals ? "same map" : "not same map");
    }
}
