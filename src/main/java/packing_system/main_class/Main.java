package packing_system.main_class;

import org.apache.spark.sql.*;
import packing_system.data_generating_system.*;
import packing_system.io.*;
import packing_system.query_system.*;

import java.io.*;
import java.util.*;

import static packing_system.api.API.*;
import static packing_system.data_generating_system.FPGrowth.*;
import static packing_system.io.CSVFileIO.*;
import static packing_system.io.MongoUtils.*;
import static packing_system.io.SharedAttributes.*;
import static packing_system.query_system.QuerySystem.*;

public class Main {

    public static void putItems(){

    }
    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        initializeItemCode();
        //按行读取训练txt文件
        BufferedReader br = new BufferedReader(new FileReader(
                "C:\\Users\\mille\\Desktop\\同步" +
                        "\\民航项目文档类\\数据集v2\\data_train.txt"));
        String line = null;
        List<List<String>> itemCodeList = new ArrayList<>();
        while ((line = br.readLine()) != null){
            String[] s = line.split("\t");
            List<String> itemCode = new ArrayList<>();
            itemCode.add('T'+s[0]);
            itemCode.addAll(Arrays.asList(s[1].split(",")));
            itemCodeList.add(itemCode);
        }

        BufferedReader testBr = new BufferedReader(new FileReader(
                "C:\\Users\\mille\\Desktop\\同步" +
                        "\\民航项目文档类\\数据集v2\\data_test.txt"));
        List<List<String>> itemCodeTestList = new ArrayList<>();
        while ((line = testBr.readLine()) != null){
            String[] s = line.split("\t");
            List<String> itemCode = new ArrayList<>();
            itemCode.add('T'+s[0]);
            itemCode.addAll(Arrays.asList(s[1].split(",")));
            itemCodeTestList.add(itemCode);
        }

        for (List<String> strings : itemCodeTestList) {
            String correctKey = strings.get(0);
            for (List<String> stringList : itemCodeList) {
                if(correctKey.equals(stringList.get(0))){
                    for(String s : strings) {
                        if (s.charAt(0)!='T'&& stringList.contains(s)) {
                            System.out.println("yes");
                        }
                    }
                }
            }
        }

        // 调用getRules方法，传入itemCodeList，得到规则集合
        HashMap<String, List<String>> rules = getRules(
                getDataSetFromStringListOfItemCode(itemCodeList));
        long lastTime;
        System.out.println("time:" + (System.currentTimeMillis()-startTime));

        List<ItemPack> itemPacks = new ArrayList<>();
        startTime = System.currentTimeMillis();
        for(List<String> list : itemCodeTestList){
            ItemPack itemPack = new ItemPack();
            String key = list.get(0);
            list.remove(0);
            for(String s : list){
                for(int k=1;k<6;k++){
                    if(ITEM_CODE_SETS.get(k).contains(s)){
                        itemPack.addOrderItem(s, k);
                        break;
                    }
                }
            }

            List<String> recommends = rules.get(key);
            if(recommends == null) {
                itemPacks.add(itemPack);
                continue;
            }

            for(String s : list){
                boolean flag = false;
                for(String t : recommends) {
                    if (s.equals(t)) {
                        System.out.println(s);
                    }
                }
            }

            //System.out.println(recommends);
            for(String s : recommends){

                for(int j=1;j<6;j++){
                    if(ITEM_CODE_SETS.get(j).contains(s)){
                        itemPack.addRecommendedItem(s, j);
                        break;
                    }
                }
            }
            itemPacks.add(itemPack);
        }
        long commonTime = System.currentTimeMillis()-startTime;


        for(int i=1;i<6;i++) {
            startTime = System.currentTimeMillis();
            double AC = 0;
            double RC = 0;
            double F1 = 0;
            int countAc = 0;
            int countRc = 0;
            for (ItemPack itemPack : itemPacks) {
                double ac = itemPack.calculateAccuracy(i);
                double rc = itemPack.calculateRecallRate(i);
                if (ac != -1) {
                    AC += ac;
                    countAc++;
                }
                if (rc != -1) {
                    RC += rc;
                    countRc++;
                }
            }
            AC /= countAc;
            RC /= countRc;
            F1 = (AC+RC)/2;
            long cost = System.currentTimeMillis() - startTime;
            System.out.println(getFullNames()[i] + "," + AC*100 + ","
                    + RC*100 + "," + F1*100+ "," + commonTime + cost);
        }
    }
}
