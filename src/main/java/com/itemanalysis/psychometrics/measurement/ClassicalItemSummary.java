/**
 * Copyright 2015 J. Patrick Meyer
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itemanalysis.psychometrics.measurement;

import com.itemanalysis.psychometrics.data.ItemType;
import com.itemanalysis.psychometrics.data.VariableAttributes;

import java.util.Formatter;
import java.util.Iterator;
import java.util.TreeMap;

public class ClassicalItemSummary {

    private boolean allCategories = true;
    private DiscriminationType discriminationType = DiscriminationType.PEARSON;
    private VariableAttributes variableAttributes = null;
    private ItemScoring itemScoring = null;
    private CategoryResponseSummary mainItemSummary = null;
    private TreeMap<Object, CategoryResponseSummary> categorySummary = null;

    public ClassicalItemSummary(VariableAttributes variableAttributes, boolean correctForSupriousness, boolean allCategories, DiscriminationType discriminationType){
        this.variableAttributes = variableAttributes;
        this.itemScoring = this.variableAttributes.getItemScoring();
        this.discriminationType = discriminationType;

        //Only use all categories if the user has requested all options and it is a binary or polytomous item.
        this.allCategories = (allCategories && this.variableAttributes.getItemType()!=ItemType.NOT_ITEM );

        //Initialize the main item statistical summary (i.e. the overall item statistics)
        mainItemSummary = new CategoryResponseSummary(variableAttributes.getName(), variableAttributes.getName(), correctForSupriousness, discriminationType);
        initializeCategories(correctForSupriousness);
    }


    private void initializeCategories(boolean biasCorrection){
        if(!allCategories) return;
        categorySummary = new TreeMap<Object, CategoryResponseSummary>();

        //initialize category summaries
        Iterator<Object> iter = itemScoring.categoryIterator();
        CategoryResponseSummary categoryResponseSummary = null;
        Object obj = null;
        while(iter.hasNext()){
            obj = iter.next();
            categoryResponseSummary = new CategoryResponseSummary(variableAttributes.getName(), obj, biasCorrection, discriminationType);
            categorySummary.put(obj, categoryResponseSummary);
        }
    }

    public void increment(String response, double testScore){
        double itemScore = variableAttributes.computeItemScore(response);
        mainItemSummary.increment(itemScore, testScore);

        if(allCategories){
            CategoryResponseSummary crs = null;
            for(Object k : categorySummary.keySet()){
                crs = categorySummary.get(k);
                crs.increment(response, testScore);
            }
        }

    }

    public void incrementDindex(String response, double testScore, double lowerCut, double upperCut){
        double itemScore = variableAttributes.computeItemScore(response);
        mainItemSummary.incrementDindex(itemScore, testScore, lowerCut, upperCut);

        if(allCategories){
            CategoryResponseSummary crs = null;
            for(Object k : categorySummary.keySet()){
                crs = categorySummary.get(k);
                crs.incrementDindex(response, testScore, lowerCut, upperCut);
            }
        }
    }

    /**
     * Create an Object array that contains the statistics for every options for this item. This method is mainly
     * used for creating printable output.
     *
     * @return array of item statistics.
     */
    public Object[] getOptionOutputArray(){
        int count = categorySummary.size()*3;
        Object[] obj = new Object[count];
        CategoryResponseSummary temp = null;
        int col = 0;
        for(Object o : categorySummary.keySet()){
            temp = categorySummary.get(o);
            obj[col++] = temp.getDifficulty();
            obj[col++] = temp.getStandardDeviation();
            obj[col++] = temp.getDiscrimination();
        }
        return obj;
    }

    public boolean allCategories(){
        return allCategories;
    }

    public int numberOfCategories(){
        if(allCategories) return categorySummary.size();
        return 0;
    }

    public double getItemDifficulty(){
        return mainItemSummary.getDifficulty();
    }

    public double getItemStandardDeviation(){
        return mainItemSummary.getStandardDeviation();
    }

    public double getItemDiscrimination(){
        return mainItemSummary.getDiscrimination();
    }

    public double getDindexLowerMean(){
        return mainItemSummary.getDindexLowerMean();
    }

    public double getDindexUpperMean(){
        return mainItemSummary.getDindexUpperMean();
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb);

        f.format("%20s", variableAttributes.getName().toString());
        f.format("%10s", "Overall");
        f.format("% 12.4f", mainItemSummary.getDifficulty());
        f.format("% 12.4f", mainItemSummary.getStandardDeviation());

        if(discriminationType==DiscriminationType.DINDEX27 || discriminationType==DiscriminationType.DINDEX33){
            double upper = mainItemSummary.getDindexUpperMean();
            double lower = mainItemSummary.getDindexLowerMean();
            double dIndex = 0;

            if(variableAttributes.hasScoring()){
                dIndex = (upper - lower)/(itemScoring.maximumPossibleScore()-itemScoring.minimumPossibleScore());
            }else{
                dIndex = (upper - lower)/(mainItemSummary.getMaximumObservedItemScore()-mainItemSummary.getMinimumObservedItemScore());
            }


            f.format("% 12.4f", upper);
            f.format("% 12.4f", lower);
            f.format("% 12.4f", dIndex);
        }else {
            f.format("% 12.4f", mainItemSummary.getDiscrimination());
        }
        f.format("%n");

        if(allCategories){
            CategoryResponseSummary crs = null;

            for(Object k : categorySummary.keySet()){
                crs = categorySummary.get(k);

                f.format("%20s", "");
                f.format("%10s", itemScoring.getCategoryScoreString(k));
                f.format("% 12.4f", crs.getDifficulty());
                f.format("% 12.4f", crs.getStandardDeviation());

                if(discriminationType==DiscriminationType.DINDEX27 || discriminationType==DiscriminationType.DINDEX33){
                    double upper = crs.getDindexUpperMean();
                    double lower = crs.getDindexLowerMean();
                    f.format("% 12.4f", upper);
                    f.format("% 12.4f", lower);
                    f.format("% 12.4f", crs.getDiscrimination());
                }else{
                    f.format("% 12.4f", crs.getDiscrimination());
                }
                f.format("%n");
            }//end loop over response options

        }//end if allCategories

        return f.toString();
    }

    public String getHeader(){
        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb);

        if(discriminationType==DiscriminationType.DINDEX27 || discriminationType==DiscriminationType.DINDEX33){
            f.format("%90s", "==========================================================================================");
            f.format("%n");
        }else{
            f.format("%66s", "==================================================================");
            f.format("%n");
        }
        f.format("%20s", "Item");
        f.format("%10s", "Option");
        f.format("%12s", "Difficulty");
        f.format("%12s", "Std. Dev.");
        if(discriminationType==DiscriminationType.DINDEX27){
            f.format("%12s", "Upper 27%");
            f.format("%12s", "Lower 27%");
            f.format("%12s", "D-Index");f.format("%n");
            f.format("%90s", "------------------------------------------------------------------------------------------");
        }else if(discriminationType==DiscriminationType.DINDEX33){
            f.format("%12s", "Upper 33%");
            f.format("%12s", "Lower 33%");
            f.format("%12s", "D-Index");f.format("%n");
            f.format("%90s", "------------------------------------------------------------------------------------------");
        }else{
            f.format("%12s", "Discrim.");f.format("%n");
            f.format("%66s", "------------------------------------------------------------------");
        }

        return f.toString();
    }

    public String getFooter(){
        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb);

        if(discriminationType==DiscriminationType.DINDEX27 || discriminationType==DiscriminationType.DINDEX33){
            f.format("%90s", "==========================================================================================");
            f.format("%n");
        }else{
            f.format("%66s", "==================================================================");
            f.format("%n");
        }
        return f.toString();
    }


}
