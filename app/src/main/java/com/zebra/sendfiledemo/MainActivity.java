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
import com.zebra.sendfiledemo.Util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import static com.zebra.sendfiledemo.ZPL.ZQ520.Calc;

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

            /* 總數 */
            int count = 20000;

            /* 拆分兩個數字 */
            int count1 = (int) (Math.random() * count);

            try {
                count1 = Integer.parseInt(getZPLFieldText());
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "轉換 Int 類型失敗" + "\n" + "Language set to ZPL", Toast.LENGTH_LONG).show();
            }
            if (count1 > count) {
                Toast.makeText(MainActivity.this, "不能大於總數" + "\n" + "Language set to ZPL", Toast.LENGTH_LONG).show();

            }

            int count2 = count - count1;

            String oldguid = Util.GUID();

            /*
            String s = "^XA\n" +
                    "~JC\n" +
                    "\n" +
                    "^LL296\n" +
                    "^PW384\n" +
                    "^LH0,0\n" +
                    "^CI26\n" +
                    "^SEE:GB18030.DAT^FS\n" +
                    "^CW1,E:HANS.TTF^FS" +
                    "\n" +
                    "^FO128,32^AAN,20,20^FDFORMTYPE^FS\n" +
                    "^FO24,96^AAN,10,10^FDMTLNO:^FS\n" +
                    "^FO96,100^AAN,10,10^FD2T459M000-000-G5^FS\n" +
                    "^FO24,168^AAN,10,10^FDCOUNT:^FS\n" +
                    "^FO96,172^AAN,10,10^FD200000 PCS^FS\n" +
                    "^FO24,240^AAN,10,10^FDCODE:^FS\n" +
                    "^FO96,244^AAN,10,10^FD20191107^FS\n" +
                    "\n" +
                    "^FO224,136^BQN,2,3^FD\n" +
                    "   W," + Util.GUID() + ",P2a-J60102,2T459M000-000-G5,20190729,WmL-J76036," + count1 + ",PCS," + oldguid + "^FS\n" +
                    "\n" +
                    "^XZ";
            */
            String s1 = "^XA\n" +
                    "~JC\n" +
                    "\n" +
                    "^LL320\n" +
                    "^PW400\n" +
                    "^LH0,0\n" +
                    "^CI26\n" +
                    "^SEE:GB18030.DAT\n" +
                    "\n" +
                    "\n" +
                    "^FO128,20^AEN,10,10^FDType^FS\n" +
                    "^FO24,62^AEN,10,10^FDNO:^FS\n" +
                    "^FO54,102^AEN,10,10^FD2T459M000-000-G5^FS\n" +
                    "^FO24,142^AEN,10,10^FDCount:^FS\n" +
                    "^FO54,182^AEN,10,10^FD200000 PCS^FS\n" +
                    "^FO24,222^AEN,10,10^FDCode:^FS\n" +
                    "^FO54,264^AEN,10,10^FD20191107^FS\n" +
                    "\n" +
                    "^FO280,202^BQN,2,2^FD\n" +
                    "   W," + Util.GUID() + ",P2a-J60102,2T459M000-000-G5,20190729,WmL-J76036," + count1 + ",PCS," + oldguid + "^FS\n" +
                    "\n" +
                    "^XZ";
            String s2 = "^XA\n" +
                    "~JC\n" +
                    "\n" +
                    "^LL320\n" +
                    "^PW400\n" +
                    "^LH0,0\n" +
                    "^CI26\n" +
                    "^SEE:GB18030.DAT\n" +
                    "\n" +
                    "\n" +
                    "^FO128,20^AEN,10,10^FDType^FS\n" +
                    "^FO24,62^AEN,10,10^FDNO:^FS\n" +
                    "^FO54,102^AEN,10,10^FD2T459M000-000-G5^FS\n" +
                    "^FO24,142^AEN,10,10^FDCount:^FS\n" +
                    "^FO54,182^AEN,10,10^FD200000 PCS^FS\n" +
                    "^FO24,222^AEN,10,10^FDCode:^FS\n" +
                    "^FO54,264^AEN,10,10^FD20191107^FS\n" +
                    "\n" +
                    "^FO280,202^BQN,2,2^FD\n" +
                    "   W," + Util.GUID() + ",P2a-J60102,2T459M000-000-G5,20190729,WmL-J76036," + count2 + ",PCS," + oldguid + "^FS\n" +
                    "\n" +
                    "^XZ";

            configLabel = (s1 + s2).getBytes();
            //configLabel = "^XA~JC^LH0,0^WDE:*.*^XZ".getBytes();
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


