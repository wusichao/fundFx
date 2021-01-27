package com.wusichao.data;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class FetchDayDayService {

    @Autowired
    private RestTemplate restTemplate;

    private List<String> cacheNames;

    public String gu() {
        Map<String, List<String>> collect = cacheNames.stream().collect(Collectors.groupingBy(n -> n));
        List<String> names = collect.entrySet().stream().sorted(Comparator.comparing(e -> -e.getValue().size())).map(e -> e.getKey() + "(" + e.getValue().size() + ")").collect(Collectors.toList());
        return JSON.toJSONString(names);
    }

    public String funds(String code) {
        String[] codesArr = {"519091", "110011", "110011", "519732",
                "519732", "161903", "003634", "002620", "519674", "001071", "008903", "519700", "270002", "213001", "001508", "161219", "001182", "000294", "002770", "163415", "005711"};
        List<String> codes = Arrays.stream(codesArr).collect(Collectors.toList());
        if (!"0".equals(code)) {
            codes = new ArrayList<>();
            codes.add(code);
        }
        System.out.println(JSON.toJSONString(codes));
        List<String> ids = getPosition(codes);

        Map<String, String> result = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();
        List<String> names = new ArrayList<>();
        ids.forEach(e -> {
            String e1 = restTemplate.getForEntity("http://push2.eastmoney.com/api/qt/slist/get?spt=1&np=3&fltt=2&invt=2&fields=f14&secid=" + e, String.class).getBody();
            String k = e1.substring(e1.lastIndexOf("f14") + 6, e1.length() - 5);
            String v = e1.substring(e1.indexOf("f14") + 6, e1.lastIndexOf("f14") - 5);
            if (result.containsKey(k)) {
                String v1 = result.get(k) + "; " + v;
                result.put(k, v1);
                count.put(k, count.get(k) + 1);
                names.add(v);
            } else {
                result.put(k, v);
                count.put(k, 1);
                names.add(v);
            }
        });

        cacheNames = names;
        int sum = count.entrySet().stream().mapToInt(v -> v.getValue()).sum();
        Map<String, String> result1 = new HashMap<>();
        result.forEach((k, v) -> {
            k = k + ",(" + divideInteger(count.get(k), sum) + "%)," + count.get(k);
            result1.put(k, v);
        });
        List<String> collect = result1.entrySet().stream().sorted(Comparator.comparing(v -> Integer.valueOf(v.getKey().substring(v.getKey().lastIndexOf(",") + 1)))).map(e -> e.getKey() + "-------" + e.getValue()).collect(Collectors.toList());
        Collections.reverse(collect);
        return JSON.toJSONString(collect);
    }

    /**
     * 根据基金代码集合，获取持仓集合
     *
     * @param codes
     * @return
     */
    private List<String> getPosition(List<String> codes) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < codes.size(); i++) {
            String c1 = restTemplate.getForEntity("http://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jjcc&code=" + codes.get(i) + "&topline=10&year=&month=9&rt=0.6468981084738423", String.class).getBody();
            String c2 = "";
            c2 = c1.substring(c1.indexOf("id='gpdmList'>") + 14, c1.indexOf("id='gpdmList'>") + 14 + 89);
            System.out.println(c2);
            String[] cs = c2.split(",");
            for (int j = 0; j < cs.length; j++) {
                ids.add(cs[j]);
            }
        }
        return ids;
    }


    public String charge(String code) {
        String[] codesArr = {"519091", "110011", "110011", "519732",
                "519732", "161903", "003634", "002620", "519674", "001071", "008903", "519700", "270002", "213001", "001508", "161219", "001182", "000294", "002770", "163415", "005711"};
        List<String> codes = Arrays.stream(codesArr).collect(Collectors.toList());
        if (!"0".equals(code)) {
            codes = new ArrayList<>();
            codes.add(code);
        }
        System.out.println(JSON.toJSONString(codes));
        List<String> ids = getPosition(codes).stream().distinct().collect(Collectors.toList());
        int sumPe = 0;
        for (int i = 0; i < ids.size(); i++) {
            String result = restTemplate.getForEntity("http://push2.eastmoney.com/api/qt/stock/get?fields=f164&secid=" + ids.get(i), String.class).getBody();
            System.out.println(result);
            int pe = Integer.valueOf(result.substring(result.indexOf("f164") + 6, result.length() - 2));
            if (pe < 0) {
                pe = 10000;
            }
            sumPe += pe;
        }

        return divideIntegerStr(sumPe, 100 * ids.size());
    }

    public String charges() {
        String[] codesArr = {"519091", "110011", "110011", "519732",
                "519732", "161903", "003634", "002620", "519674", "001071", "008903", "519700", "270002", "213001", "001508", "161219", "001182", "000294", "002770", "163415", "005711"};
        List<String> codes = Arrays.stream(codesArr).collect(Collectors.toList());
        Map<String, String> result = new HashMap<>();
        codes.forEach(k -> {
            String v = charge(k);
            result.put(k, v);
        });
        List<Map.Entry<String, String>> list = new ArrayList<>(result.entrySet());
        //升序排序
        Collections.sort(list, Comparator.comparing(o -> Float.valueOf(o.getValue())));
        return JSON.toJSONString(list);
    }

    public Integer divideInteger(Integer a, Integer b) {
        if (b == null || b == 0) {
            return 0;
        }
        return BigDecimal.valueOf(a * 100).divide(BigDecimal.valueOf(b), 2, BigDecimal.ROUND_DOWN).intValue();
    }

    public static String divideIntegerStr(Integer a, Integer b) {
        if (b == null || b == 0) {
            return "0";
        }
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 2, BigDecimal.ROUND_DOWN).toString();
    }
}
