/*
 * Copyright 2014-2020 Sayi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.deepoove.poi.policy.reference;

import java.util.List;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData.Series;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xwpf.usermodel.XWPFChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;

import com.deepoove.poi.data.SeriesRenderData;
import com.deepoove.poi.template.ChartTemplate;
import com.deepoove.poi.util.ReflectionUtils;

public abstract class AbstractChartTemplateRenderPolicy<T> extends AbstractTemplateRenderPolicy<ChartTemplate, T> {

    protected final int FIRST_ROW = 1;
    protected final int CATEGORY_COL = 0;
    protected final int VALUE_START_COL = 1;

    protected <N extends Number> XDDFNumericalDataSource<Number> createValueDataSource(XWPFChart chart, N[] data,
            int index) {
        return XDDFDataSourcesFactory.fromArray(data,
                chart.formatRange(
                        new CellRangeAddress(FIRST_ROW, data.length, index + VALUE_START_COL, index + VALUE_START_COL)),
                index + VALUE_START_COL);
    }

    protected XDDFCategoryDataSource createCategoryDataSource(XWPFChart chart, String[] categories) {
        return XDDFDataSourcesFactory.fromArray(categories,
                chart.formatRange(new CellRangeAddress(FIRST_ROW, categories.length, CATEGORY_COL, CATEGORY_COL)),
                CATEGORY_COL);
    }

    protected void removeExtraSeries(final XDDFChartData chartData, XSSFSheet sheet, final int numOfPoints,
            final List<Series> orignSeries, final int seriesSize) {
        final int orignSize = orignSeries.size();
        if (orignSize - seriesSize > 0) {
            Object intenalChart = ReflectionUtils.getValue("chart", chartData);
            for (int j = orignSize - 1; j >= seriesSize; j--) {
                orignSeries.remove(j);
                if (null != intenalChart) {
                    if (intenalChart instanceof CTBarChart) {
                        ((CTBarChart) intenalChart).removeSer(j);
                    } else if (intenalChart instanceof CTLineChart) {
                        ((CTLineChart) intenalChart).removeSer(j);
                    }
                }
            }
            // clear extra sheet column
            for (int i = 0; i < numOfPoints + 1; i++) {
                for (int j = orignSize; j > seriesSize; j--) {
                    XSSFRow row = sheet.getRow(i);
                    if (null == row) continue;
                    XSSFCell cell = row.getCell(j);
                    if (null != cell) row.removeCell(cell);
                }
            }
        }
    }

    protected void updateCTTable(XSSFSheet sheet, List<SeriesRenderData> seriesDatas) {
        final int seriesSize = seriesDatas.size();
        final int numOfPoints = seriesDatas.get(0).getData().length;
        CTTable ctTable = getSheetTable(sheet);
        ctTable.setRef("A1:" + (char) ('A' + seriesSize) + (numOfPoints + 1));
        CTTableColumns tableColumns = ctTable.getTableColumns();
        tableColumns.setCount(seriesSize + 1);

        int size = tableColumns.sizeOfTableColumnArray();
        for (int i = size - 1; i >= 0; i--) {
            tableColumns.removeTableColumn(i);
        }

        CTTableColumn column = tableColumns.addNewTableColumn();
        // category
        column.setId(1);
        column.setName(" ");
        // series
        for (int i = 0; i < seriesSize; i++) {
            column = tableColumns.addNewTableColumn();
            column.setId(1 + i + 1);
            column.setName(seriesDatas.get(i).getName());
        }
    }

    protected CTTable getSheetTable(XSSFSheet sheet) {
        if (sheet.getTables().size() == 0) {
            XSSFTable newTable = sheet.createTable(null);
            newTable.getCTTable().addNewTableColumns();
            sheet.getTables().add(newTable);
        }
        return sheet.getTables().get(0).getCTTable();
    }

}
