package com.xue.k9track.util;

/**
 * 半正矢公式 (Haversine Formula) 工具类
 * 用于计算地球表面两点之间的大圆距离
 */
public class HaversineUtil {

    /** 地球平均半径 (km) */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * 计算两点间的大圆距离 (km)
     *
     * @param lat1 起点纬度
     * @param lng1 起点经度
     * @param lat2 终点纬度
     * @param lng2 终点经度
     * @return 两点间距离 (km)
     */
    public static double distance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
