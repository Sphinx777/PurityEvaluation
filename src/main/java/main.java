import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by user on 2017/5/28.
 */
public class main {
    //test args
    /*-modelPath
    D:\MySyncFolder\essay\MainPaper\resultInput3.csv
    -inputPath
    D:\MySyncFolder\essay\MainPaper\tNMijF_result_1\topic100\result_2017-0621-025953\result\topic100.topWords*/
    public static void main(String[] args){
        CmdArgs cmdArgs = new CmdArgs();
        CmdLineParser parser = new CmdLineParser(cmdArgs);
        LinkedHashMap<String,String> columnHeaderMap = new LinkedHashMap<String, String>();
        LinkedHashMap<String,ArrayList<String>> oldColumnValueMap = new LinkedHashMap<String, ArrayList<String>>();

        try{
            parser.parseArgument(args);
            System.out.println("modelPath:"+cmdArgs.modelPath);
            System.out.println("inputPath:"+cmdArgs.inputPath);

            if(!cmdArgs.modelPath.trim().equals("")){
                //1. read modelPath
                try(BufferedReader br = new BufferedReader(new FileReader(cmdArgs.modelPath))){
                    String line;
                    while ((line=br.readLine()) != null){
                        //is numeric
                        //2. read modelPath file content
                        if(StringUtils.isNumeric(line.substring(line.length()-1))){
                            //read table content
                            String[] splitStrings = line.split(Contants.LINE_DELIMITER);
                            if(!splitStrings[0].trim().equals("")){
                                for(String key:columnHeaderMap.keySet()){
                                    if(!splitStrings[Integer.parseInt(key)].trim().equals("")){
                                        String[] cellTokens = splitStrings[Integer.parseInt(key)].split(Contants.CELL_DELIMITER);
                                        for(String token:cellTokens){
                                            ((ArrayList<String>)oldColumnValueMap.get(key)).add(token);
                                        }
                                    }
                                }
                            }
                        }else{
                            //read table schema header and reset
                            if(columnHeaderMap.size()==0){
                                String[] splitStrings = line.split(Contants.LINE_DELIMITER);
                                System.out.println("method:"+splitStrings[0]);
                                System.out.println("dataSet:"+splitStrings[1]);
                                System.out.println("topic number:"+splitStrings[2]);

                                //put columnHeaderMap, initialize oldColumnValueMap
                                for(int i=3;i<splitStrings.length;i++){
                                    if(!splitStrings[i].trim().equals("")){
                                        columnHeaderMap.put(String.valueOf(i),splitStrings[i]);
                                        oldColumnValueMap.put(String.valueOf(i),new ArrayList<String>());
                                    }
                                }
                            }
                        }
                    }
                }catch (IOException ex){
                    ex.printStackTrace();
                }

                int totalClusters =0;
                int maxClusterWords =0;
                ArrayList<LinkedHashMap<String,ArrayList<String>>> newColumnValueList = new ArrayList<>();
                ArrayList<String> lines = new ArrayList<>();
                String tableSchemaString =StringUtils.join(columnHeaderMap.values().toArray(),Contants.LINE_DELIMITER);
                //add table schema
                lines.add(",,,,,"+tableSchemaString);

                //3. read inputPath
                try(BufferedReader br = new BufferedReader(new FileReader(cmdArgs.inputPath))){
                    String line ;
                    ArrayList<String> newIdxList = new ArrayList<>();
                    while ((line=br.readLine()) != null){
                        totalClusters++;
                        String[] splitStrings = line.split(Contants.SPACE_DELIMITER);
                        LinkedHashMap<String,ArrayList<String>> newColumnValueMap = new LinkedHashMap<String, ArrayList<String>>();
                        for(String token:splitStrings){
                            //check old map contain
                            newIdxList = addToHashMap(token, oldColumnValueMap,columnHeaderMap);
                            for(String idx:newIdxList) {
                                if (newColumnValueMap.containsKey(idx)) {
                                    ((ArrayList<String>) newColumnValueMap.get(idx)).add(token);
                                } else {
                                    ArrayList<String> hashSet = new ArrayList<>();
                                    hashSet.add(token);
                                    newColumnValueMap.put(idx, hashSet);
                                }
                            }
                        }

                        int maxCnt=-999;
                        ArrayList<String> stringArrayList = new ArrayList();

                        for(String idx:columnHeaderMap.keySet()){
                            if(newColumnValueMap.containsKey(idx)){
                                if(newColumnValueMap.get(idx).size()>maxCnt){
                                    maxCnt = newColumnValueMap.get(idx).size();
                                }
                                String cellString = StringUtils.join(newColumnValueMap.get(idx).toArray(),Contants.CELL_DELIMITER);
                                stringArrayList.add(cellString);
                            }else{
                                if(idx.equals(columnHeaderMap.keySet().toArray()[columnHeaderMap.size()-1])){
                                    stringArrayList.add(String.valueOf(maxCnt));
                                }else{
                                    stringArrayList.add("");
                                }
                            }
                        }

//                        for(ArrayList<String> hashSet:newColumnValueMap.values()){
//                            if(hashSet.size()>maxCnt){
//                                maxCnt = hashSet.size();
//                            }
//                            String cellString = StringUtils.join(hashSet.toArray(),Contants.CELL_DELIMITER);
//                            stringArrayList.add(cellString);
//                        }
                        maxClusterWords += maxCnt;
                        newColumnValueList.add(newColumnValueMap);
                        lines.add(line+",,,,,"+StringUtils.join(stringArrayList.toArray(),Contants.LINE_DELIMITER));
                    }
                }catch (IOException ex){
                    ex.printStackTrace();
                }

                //4. calculate the purity value
                int totalWords = totalClusters * 10;
                double purityValue = maxClusterWords / (double)totalWords;
                System.out.println("purity value:"+purityValue);
                lines.add(",,,,,,,,,,,,"+purityValue);

                //5. write out the file at the outputPath
                Path file = Paths.get(cmdArgs.inputPath+"_purity.csv");
                Files.write(file,lines, Charset.defaultCharset());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static ArrayList<String> addToHashMap(String token,LinkedHashMap<String,ArrayList<String>> columnValueMap,LinkedHashMap<String,String> columnHeaderMap){
        ArrayList<String> strResult = new ArrayList<>();
        //1. containValue
        //strResult = columnValueMap.containsValue(token);

        //2. iterate hashMap Valus
        for (Map.Entry<String,ArrayList<String>> entry:columnValueMap.entrySet()){
            if(entry.getValue().contains(token)){
                strResult.add(entry.getKey());
                //break;
            }
        }

        if(strResult.size()==0){
            String etcIdx="";
            for (Map.Entry entry:columnHeaderMap.entrySet()){
                if(entry.getValue().equals("etc")){
                    etcIdx = (String) entry.getKey();
                    break;
                }
            }

            if(!etcIdx.equals("")) {
                columnValueMap.get(etcIdx).add(token);
                strResult.add(etcIdx);
            }
        }

        //output the coherence header idx or the etc header idx
        return strResult;
    }
}
