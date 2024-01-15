package com.jp.calefaction.service.charting;

import com.jp.calefaction.model.chart.TempChartData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.stereotype.Service;

@Service
public class JFreeChartService {

    public JFreeChart createTemperatureChart(List<TempChartData> dataList, String timeZone, String unit) {
        TimeSeries series = new TimeSeries("Temperature");

        for (TempChartData data : dataList) {
            Date date = new Date(data.getDt() * 1000L);
            series.add(new Hour(date), data.getTemp());
            // series.add(new Second(new Date(data.getDt() * 1000)), data.getTemp());
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Temperature Over Time", // Title
                "Time", // X-axis Label
                "Temperature (" + unit + ")", // Y-axis Label
                dataset, // Dataset
                true, // Show Legend
                true, // Use tooltips
                false // URLs?
                );

        XYPlot plot = (XYPlot) chart.getPlot();
        DateAxis axis = (DateAxis) plot.getDomainAxis();

        // Set the date format for the axis
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        axis.setDateFormatOverride(dateFormat);
        return chart;
    }
}
