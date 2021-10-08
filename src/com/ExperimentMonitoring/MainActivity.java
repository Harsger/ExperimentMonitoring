package com.ExperimentMonitoring;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Date;
import java.lang.String;
import java.lang.Runtime;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.SimpleDateFormat;

import com.jcraft.jsch.*;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.view.View;
import android.text.method.ScrollingMovementMethod;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Color;

class GlobVar{
    public static double komma ;
    public static int number ;
    public static String word ;
}

class PlotCreator{
    public ArrayList< Double[] > valuePairs ;
    public PlotCreator(){
        this.valuePairs = new ArrayList< Double[] >() ;
    }
    public void fill( Double x , Double y ){
        Double[] filler = { x , y } ;
        this.valuePairs.add( filler ) ;
    }
    public Bitmap overwriteBackground( Bitmap background ){
    
        Bitmap plot = background.copy( background.getConfig() , true ) ;
    
        Integer[] imageSize = new Integer[]{
            background.getWidth() ,
            background.getHeight() 
        } ;
        
        int entries = this.valuePairs.size() ;

        if( entries < 2 ) return plot ; 

        Double[][] minMax = { 
                { this.valuePairs.get(0)[0] , this.valuePairs.get(0)[0] } ,
                { this.valuePairs.get(0)[1] , this.valuePairs.get(0)[1] } 
            };
        
        for(int i=0; i<entries; i++){
            for(int c=0; c<2; c++){
                if( minMax[c][0] > this.valuePairs.get(i)[c] ) 
                    minMax[c][0] = this.valuePairs.get(i)[c] ;
                else if( minMax[c][1] < this.valuePairs.get(i)[c] ) 
                    minMax[c][1] = this.valuePairs.get(i)[c] ;
            }
        }
        
        Double[] range = {
            minMax[0][1] - minMax[0][0] ,
            minMax[1][1] - minMax[1][0] 
        } ;

        if( range[0] == 0. ) return plot ; 

        if( range[1] == 0. ){
            minMax[1][0] -= 1. ;
            minMax[1][0] += 1. ;
            range[1] = 2. ;
        }
        
        GlobVar.komma = range[1] ;
        
        for(int i=0; i<entries; i++){
        
            int x = (int)( 
                            (double)(imageSize[0] - 1)
                            * ( this.valuePairs.get(i)[0] - minMax[0][0] ) 
                             / range[0] 
                        ) ;
                        
            int y = (int)( 
                            (double)(imageSize[1] - 1) 
                            *
                            (   
                                1. -
                                ( this.valuePairs.get(i)[1] - minMax[1][0] ) 
                                / range[1] 
                            )
                        ) ;
                        
            if( 
                x < 0 || x > imageSize[0]-1 
                || 
                y < 0 || y > imageSize[1]-1 
            )
                continue ;
                
            plot.setPixel( x , y , Color.WHITE );
            
        }
        
        return plot ;
        
    }
}

public class MainActivity extends Activity {

    String output = " no response yet ";
    String command = "touch veryLongUselessName.datei";
    String line = " to be filled ";
    
    Integer[] imageSize = new Integer[]{ 360 , 100 } ;
    
    Bitmap emptyField = Bitmap.createBitmap( 
                                            imageSize[0], 
                                            imageSize[1], 
                                            Bitmap.Config.ARGB_8888
                                        ); 
                                        
    Date date ;
    SimpleDateFormat sdf = 
        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") ;

    JSch javaSecureChannel;
    Session session;
    ChannelExec channel;
    
    java.util.Properties config = new java.util.Properties();
    
    BufferedReader stdout;
    BufferedReader stderr;
    
    boolean success = false;
    int TIMEOUT = 60000;
    Integer port = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        for(int c=0; c<imageSize[0]; c++){
            emptyField.setPixel( c , 0 , Color.GRAY );
            emptyField.setPixel( c , imageSize[1]-1 , Color.GRAY );
        }
        for(int c=0; c<imageSize[1]; c++){
            emptyField.setPixel( 0 , c , Color.GRAY );
            emptyField.setPixel( imageSize[0]-1 , c , Color.GRAY );
        }
        
        final HashMap< String , PlotCreator > data = 
            new HashMap< String , PlotCreator >(){{
        
            put( "A" , new PlotCreator() ) ;
            put( "B" , new PlotCreator() ) ;
            put( "C" , new PlotCreator() ) ;
        
        }};
        
        final HashMap< String , ImageView > outputCurves = 
            new HashMap< String , ImageView >(){{
        
            put( "A" , (ImageView)findViewById(R.id.Acurve ) );
            put( "B" , (ImageView)findViewById(R.id.Bcurve ) );
            put( "C" , (ImageView)findViewById(R.id.Ccurve ) );
        
        }};
        
        final HashMap< String , TextView > outputTexts = 
            new HashMap< String , TextView >(){{
        
            put( "A" , (TextView)findViewById(R.id.Atext ) );
            put( "B" , (TextView)findViewById(R.id.Btext ) );
            put( "C" , (TextView)findViewById(R.id.Ctext ) );
            
            put( "debugText" , (TextView)findViewById(R.id.debugText ) );
        
        }};
        
        final HashMap< String , EditText > inputTexts = 
            new HashMap< String , EditText >(){{
        
            put( "A" , (EditText)findViewById(R.id.Ainput ) );
            put( "B" , (EditText)findViewById(R.id.Binput ) );
            put( "C" , (EditText)findViewById(R.id.Cinput ) );
        
            put( "login"    , (EditText)findViewById(R.id.loginInput    ) );
            put( "username" , (EditText)findViewById(R.id.usernameInput ) );
            put( "password" , (EditText)findViewById(R.id.passwordInput ) );
            put( "node"     , (EditText)findViewById(R.id.nodeInput     ) );
            put( "date"     , (EditText)findViewById(R.id.dateInput     ) );
            put( "duration" , (EditText)findViewById(R.id.durationInput ) );
            put( "back"     , (EditText)findViewById(R.id.backInput     ) );
        
        }};
        
        final Button requestButton = (Button)findViewById(R.id.request);
        
        for( HashMap.Entry< String, PlotCreator > entry : data.entrySet() ){
            outputTexts.get( entry.getKey()  )
                       .setMovementMethod(new ScrollingMovementMethod()); 
            outputCurves.get( entry.getKey()  ).setImageBitmap(
                emptyField.copy( emptyField.getConfig() , true ) );
        }
        
        outputTexts.get( "debugText"  )
                    .setMovementMethod(new ScrollingMovementMethod()); 
        
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            
                requestButton.setClickable(false);
                
                outputTexts.get("A").setText( "REQUESTING" );
                                
                output = "";
                        
                success = false;
                session = null;
                
                try {
                
                    javaSecureChannel = new JSch();
                
                    session = javaSecureChannel.getSession( 
                                                    inputTexts
                                                        .get( "username" )
                                                        .getText()
                                                        .toString() , 
                                                    inputTexts
                                                        .get( "login" )
                                                        .getText()
                                                        .toString() , 
                                                    port 
                                                );

                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);

                    session.setConfig( new Properties() );
                    session.setUserInfo( null );
                    
                    session.setPassword( 
                                            inputTexts
                                                .get( "password" )
                                                .getText()
                                                .toString() 
                                        );

                    session.connect(TIMEOUT);
                    
                    success = true;
                    
                } catch (JSchException secureChannelException) {
                    output += " EXCEPTION : at session connection ";
                    output += "\n";
                    output += secureChannelException
                                                    .getLocalizedMessage()
                                                    .toString();
                }
                
                if( success ){
                
                    ChannelExec channel = null;
                    
                    try {
                    
                        channel = (ChannelExec) session.openChannel("exec");
                        
                        command = "cd ~/ExperimentMonitoring/remote" ;
                        command += " && " ;
                        command += "./copyNewestData.sh " ;
                        command += " " ;
                        command += inputTexts.get("node")
                                             .getText().toString() ;
                        command += " '" ;
                        command += inputTexts.get("date")
                                             .getText().toString() ;
                        command += "' && " ;
                        command += "./logStats " ;
                        
                        outputTexts.get("debugText")
                                   .setText( "SENDING" );
                
                        channel.setCommand( command );

                        stdout = new BufferedReader( 
                                    new InputStreamReader( 
                                            channel.getInputStream() 
                                        ) 
                                    );
                        stderr = new BufferedReader( 
                                    new InputStreamReader( 
                                            channel.getErrStream() 
                                        ) 
                                    );
                                    
                        outputTexts.get("debugText")
                                   .setText( "RECEIVING" );

                        channel.connect(TIMEOUT);
                        success = true;
                    
                        while ((line = stdout.readLine()) != null) {
                            output += line;
                            output += "\n";
                        }
                        
                        while ((line = stderr.readLine()) != null) {
                            output += line;
                            output += "\n";
                        }
                                    
                        outputTexts.get("debugText").setText( output );
                        
                        for( 
                                HashMap.Entry< String , PlotCreator > entry 
                                : 
                                data.entrySet() 
                        ){
                        
                            output = "" ;
                        
                            if( channel != null ) channel.disconnect();
                            
                            channel = (ChannelExec) session.openChannel("exec");
                            
                            command = "cd ~/ExperimentMonitoring/remote" ;
                            command += " && " ;
                            command += "./logStats tempData.dat " ;
                            command += inputTexts
                                                 .get( entry.getKey() )
                                                 .getText().toString() ;
                            command += " " ;
                            command += inputTexts
                                                 .get( "duration" )
                                                 .getText().toString() ;
                            command += " " ;
                            command += inputTexts
                                                 .get( "back" )
                                                 .getText().toString() ;
                    
                            channel.setCommand( command );

                            stdout = new BufferedReader( 
                                        new InputStreamReader( 
                                                channel.getInputStream() 
                                            ) 
                                        );
                            stderr = new BufferedReader( 
                                        new InputStreamReader( 
                                                channel.getErrStream() 
                                            ) 
                                        );

                            channel.connect(TIMEOUT);
                        
                            while ((line = stdout.readLine()) != null) {
                                output += line;
                                output += "\n";
                            }
                            
                            while ((line = stderr.readLine()) != null) {
                                output += line;
                                output += "\n";
                            }
                
                            outputTexts.get(entry.getKey()).setText( output );
                        
                            if( channel != null ) channel.disconnect();
                            
                            channel = (ChannelExec) session.openChannel("exec");
                            
                            command = "cd ~/ExperimentMonitoring/remote" ;
                            command += " && " ;
                            command += "./logCondensed tempData.dat " ;
                            command += inputTexts
                                                 .get( entry.getKey() )
                                                 .getText().toString() ;
                            command += " " ;
                            command += inputTexts
                                                 .get( "duration" )
                                                 .getText().toString() ;
                            command += " " ;
                            command += inputTexts
                                                 .get( "back" )
                                                 .getText().toString() ;
                    
                            channel.setCommand( command );

                            stdout = new BufferedReader( 
                                        new InputStreamReader( 
                                                channel.getInputStream() 
                                            ) 
                                        );
                            stderr = new BufferedReader( 
                                        new InputStreamReader( 
                                                channel.getErrStream() 
                                            ) 
                                        );

                            channel.connect(TIMEOUT);
                        
                            while ((line = stdout.readLine()) != null) {
                                String[] words = line.split(" ");
                                if( words.length != 2 ) continue ;
                                double x = Double.parseDouble( words[0] ) ;
                                double y = Double.parseDouble( words[1] ) ;
                                entry.getValue().fill( x , y ) ;
                            }

                            if( entry.getValue().valuePairs.size() < 2 ) 
                                continue ;
                            
                            outputCurves.get( entry.getKey() )
                                        .setImageBitmap( 
                                            entry.getValue()
                                                 .overwriteBackground( 
                                                    emptyField 
                                                 ) 
                                        ) ;
                                        
                            int nPoints = data.get(entry.getKey())
                                              .valuePairs.size() ;
                            if( nPoints > 0 ){
                                date = new java.util.Date(
                                    (long)data.get(entry.getKey())
                                              .valuePairs.get(nPoints-1)[0]
                                              .doubleValue()
                                    *1000L
                                ) ;
                                output += " ";
                                output += sdf.format( date ) ;
                                output += "\n";
                                outputTexts.get(entry.getKey())
                                           .setText( output );
                            }
                                        
                            data.get(entry.getKey()).valuePairs.clear() ;
                        
                        }
                        
                    } catch( IOException | JSchException secureChannelException ) {
                    
                        if( session != null ) session.disconnect();
                        output = " EXCEPTION : at channel connection ";
                        output += "\n";
                        output += secureChannelException
                                                        .getLocalizedMessage()
                                                        .toString();
                                    
                        outputTexts.get("debugText").setText( output );
                        
                    } finally {
                    
                        if( channel != null ) channel.disconnect();
                        if( session != null ) session.disconnect();
                        
                    }
                
                }
        
                requestButton.setClickable(true);
                    
            }
                
        } );
        
    }
    
}
