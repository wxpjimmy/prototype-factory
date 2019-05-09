package com.jimmy.prototype.jackson;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import java.io.IOException;

public class JacksonFilterTest {
    @JsonFilter("myFilter")
    public interface MyFilter {}
    public static void main(String[] args) throws IOException {
        //property filter
        Person p = new Person();
        p.setAge(29);
        p.setCountry("china");
        p.setName("allen");
        p.setGender("male");
        p.setDesc("programmer");

        ObjectMapper mapper = new ObjectMapper();
        String content = mapper.writeValueAsString(p);
        System.out.println(content);

        Person pp = mapper.readValue(content, Person.class);
        System.out.println(pp);

        //
        //设置 addMixIn
        mapper.addMixIn(Person.class, MyFilter.class);
        //调用 SimpleBeanPropertyFilter 的 serializeAllExcept 方法
//        SimpleBeanPropertyFilter newFilter =
//                SimpleBeanPropertyFilter.serializeAllExcept("name");
        //或重写 SimpleBeanPropertyFilter 的 serializeAsField 方法
        SimpleBeanPropertyFilter newFilter = new SimpleBeanPropertyFilter() {
            @Override
            public void serializeAsField(Object pojo, JsonGenerator jgen,
                                         SerializerProvider provider, PropertyWriter writer)
                    throws Exception {
                if (!writer.getName().equals("name")) {
                    writer.serializeAsField(pojo, jgen, provider);
                }
            }
        };
        //设置 FilterProvider
        FilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("myFilter", newFilter);
        content = mapper.setFilterProvider(filterProvider).writeValueAsString(p);
        System.out.println(content);
    }
}
