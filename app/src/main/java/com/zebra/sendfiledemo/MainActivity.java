package com.zebra.sendfiledemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.device.SmartcardReader;
import com.zebra.sdk.device.SmartcardReaderFactory;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends ConnectionScreen {
    private UIHelper helper = new UIHelper(this);

    private ProgressDialog myDialog;
    private Connection connection = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        testButton.setText("Send Test File");


    }

    @Override
    public void performTest() {
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                connectAndGetData();
                Looper.loop();
                Looper.myLooper().quit();
            }
        }).start();

    }

    /**
     * Check for the printer status and language and send the test file to the printer,implements best practices to show status of the printer.
     */

    private void connectAndGetData() {

        if (isBluetoothSelected() == false) {
            try {
                connection = new TcpConnection(getTcpAddress(), Integer.valueOf(getTcpPortNumber()));
            } catch (NumberFormatException e) {
                helper.showErrorDialogOnGuiThread("Port number is invalid");
                return;
            }
        } else {
            connection = new BluetoothConnection(getMacAddressFieldText());
        }
        try {
            helper.showLoadingDialog("Connecting.....");
            connection.open();
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
            ZebraPrinterLinkOs linkOsPrinter = ZebraPrinterFactory.createLinkOsPrinter(printer);
            PrinterStatus printerStatus = (linkOsPrinter != null) ? linkOsPrinter.getCurrentStatus() : printer.getCurrentStatus();
            getPrinterStatus();

            if (printerStatus.isReadyToPrint) {
                testSendFile(printer);
            } else if (printerStatus.isHeadOpen) {
                helper.showErrorDialogOnGuiThread("Head Open \nPlease Close Printer Head to Print.");
            } else if (printerStatus.isPaused) {
                helper.showErrorDialogOnGuiThread("Printer Paused.");
            } else if (printerStatus.isPaperOut) {
                helper.showErrorDialogOnGuiThread("Media Out \nPlease Load Media to Print.");
            }
            connection.close();
            saveSettings();
        } catch (ConnectionException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ZebraPrinterLanguageUnknownException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } finally {
            helper.dismissLoadingDialog();
        }
    }


    /**
     * This method implements best practices to check the language of the printer and set the language of the printer to ZPL.
     *
     * @return printer
     * @throws ConnectionException
     */
    private void getPrinterStatus() throws ConnectionException {

        final String printerLanguage = SGD.GET("device.languages", connection);

        final String displayPrinterLanguage = "Printer Language is " + printerLanguage;

        SGD.SET("device.languages", "hybrid_xml_zpl", connection);

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(MainActivity.this, displayPrinterLanguage + "\n" + "Language set to ZPL", Toast.LENGTH_LONG).show();

            }
        });


    }

    private void testSendFile(ZebraPrinter printer) {
        try {
            File filepath = getFileStreamPath("TEST.LBL");
            createDemoFile(printer, "TEST.LBL");
            printer.sendFileContents(filepath.getAbsolutePath());
            SettingsHelper.saveBluetoothAddress(this, getMacAddressFieldText());
            SettingsHelper.saveIp(this, getTcpAddress());
            SettingsHelper.savePort(this, getTcpPortNumber());

        } catch (ConnectionException e1) {
            helper.showErrorDialogOnGuiThread("Error sending file to printer");
        } catch (IOException e) {
            helper.showErrorDialogOnGuiThread("Error creating file");
        }
    }

    /**
     * This method includes the creation of test file in ZPL and CPCL formats.
     *
     * @param printer
     * @param fileName
     * @throws IOException
     */

    private void createDemoFile(ZebraPrinter printer, String fileName) throws IOException {

        FileOutputStream os = this.openFileOutput(fileName, Context.MODE_PRIVATE);

        byte[] configLabel = null;

        PrinterLanguage pl = printer.getPrinterControlLanguage();
        if (pl == PrinterLanguage.ZPL) {
            //configLabel = "^XA^FO17,16^GB379,371,8^FS^FT65,255^A0N,135,134^FDTEST^FS^XZ".getBytes();
            StringBuilder builder = new StringBuilder();

            builder.append("^XA");
            /*文本打印 start*/

            builder.append("^LH40,40^FS");//原点位置
            builder.append("^LL300^FS");//标签长度
            builder.append("^FO20,10");//文本/条码位置
            builder.append("^MD10");//浅暗度
            builder.append("^AEN,60,30");//字体类型大小
            builder.append("^FD" + "Foxconn" + "^FS");//文本内容

            /*条码打印 start*/
            builder.append("^FO20,80");//文本/条码位置
            builder.append("^MD20");//浅暗度
            builder.append("^BY3,2.4,50");//条形码系统设定（默认）
            builder.append("^B3N,Y,20,N,N");//Code 39
            builder.append("^FD" + "6971458916004" + "^FS");//条形码数据

            /*文本打印*/
            builder.append("^FO20,100");
            builder.append("^MD25");//浅暗度
            builder.append("^AEN,80,80");
            builder.append("^FD" + getZPLFieldText() + "^FS");


            /*汉字打印*/
            builder.append("^FO20,160");
            builder.append("^MD10");//浅暗度
            builder.append("^BQ,2,10");
            builder.append("^CW1,E:SIMSUN.FNT");//字体（宋体）
            builder.append("^CI26");
            builder.append("^FD中文^FS") ;//打印文字

            /*二维码打印*/
            builder.append("^FO80,160");
            builder.append("^FT448,288^BQ2,2,10^A1N,48,48");//浅暗度
            builder.append("^FD中文^FS") ;//打印文字
            builder.append("^FD中文^FS");  //打印二维码

            builder.append("^XZ");

            configLabel = builder.toString().getBytes();
        } else if (pl == PrinterLanguage.CPCL) {
            String cpclConfigLabel = "! 0 200 200 406 1\r\n" + "ON-FEED IGNORE\r\n" + "BOX 20 20 380 380 8\r\n" + "T 0 6 137 177 TEST\r\n" + "PRINT\r\n";
            configLabel = cpclConfigLabel.getBytes();
        }
        os.write(configLabel);
        os.flush();
        os.close();
    }

    private void saveSettings() {
        SettingsHelper.saveBluetoothAddress(MainActivity.this, getMacAddressFieldText());
        SettingsHelper.saveIp(MainActivity.this, getTcpAddress());
        SettingsHelper.savePort(MainActivity.this, getTcpPortNumber());
    }

}


