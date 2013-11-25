# Open Bluetooth GPS Provider

通过蓝牙（Bluetooth）连接 NMEA GPS 兼容设备，从而让设备具有地理定位功能。


## 特性

* 支持市面上所有 NMEA 兼容的蓝牙 GPS 设备
* 自动判断连接，节省电力和内存
* 开放源代码以及访问接口，随时方便调用


## 如何使用

### 系统需求

* 需要蓝牙设备的支持
* Android 4.0 以及以上版本

打开蓝牙，并配对您的 GPS 设备；然后在「设置」中打开「开发者选项」中的「允许模拟位置」。

然后打开本应用，选择需要链接的设备，点击「连接」，连接成功以后如果需要其它应用访问蓝牙 GPS 信息，则在关闭本机 GPS 设备的同时（如果有的话）然后勾选「开启模拟位置」选项按钮。

本应用会在后台保持运行，并随时可以在通知中心关闭。为了保证正确的蓝牙通讯，同时请务必保持后台服务正常运行和开启（放心，这不会耗费太多的资源）。


### 开发者信息

如果您想保留本机 GPS 访问的同时，增加蓝牙 GPS 的定位辅助，您可以直接访问名为 `btgps` 的 `Location Provider` （始终添加）。示例代码如下：

        mLocationManager.requestLocationUpdates("btgps", 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        });
        
同时，您可以通过系统广播调用本程序的功能，例如开启和关闭蓝牙 GPS 连接、关闭和开启模拟位置。本应用使用到的广播如下

* bluetoothgps.action.updatelocation // 收到 GPS 位置信息
* bluetoothgps.action.connect.failed  // GPS 设备连接失败
* bluetoothgps.action.connect.success  // GPS 设备连接失败
* bluetoothgps.action.disconnected  // GPS 设备失去连接
* bluetoothgps.action.connected  // GPS 设备已连接
* bluetoothgps.action.provider.add  // 已增加 Location 的 Provider
* bluetoothgps.action.provider.remove  // 已删除 Location 的 Provider
    
您可以查看 `BluetootGPS` 类获得所有的系统调用广播。


## 常见问题

### 为什么有时候无法连接蓝牙 GPS 设备？

有很多情况都无法连接 GPS 设备，请检查是否有如下的情况：

1. GPS 设备没有安全的关闭访问
2. 本应用异常退出

一般情况下，您可以关闭 GPS 设备，然后开启重新连接即可解决问题。如果问题仍然存在，则可以考虑重置 GPS 设备（根据不同设备的型号重置的方式不同），然后开关安卓设备的蓝牙再尝试重新连接。


## 历史

### 2013-11-25

初始化版本，完成基本功能

