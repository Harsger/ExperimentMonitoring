#include "useful.h"

using namespace std;

int main(int argc, char *argv[]){
    
    if( argc < 4 ) return 1 ;
    
    string filename  = argv[1] ;
    string specifier = argv[2] ;
    string quantity  = argv[3] ;
    
    SpecifiedNumber duration ;
    SpecifiedNumber timeRange ;
    
    if( argc > 4 ){
        string unit ;
        duration = 
            SpecifiedNumber( getNumberWithUnit( argv[4] , unit ) ) ;
        duration.specifier = unit ;
    }
    
    if( argc > 5 ){
        string unit ;
        timeRange = 
            SpecifiedNumber( getNumberWithUnit( argv[5] , unit ) );
        timeRange.specifier = unit ;
    }
    
    unsigned int timeInterval = secondsPER["d"] ;
    unsigned int timeTOaverage = secondsPER["m"] ;
    if( 
        duration.setting 
        &&
        ! toDiscard( duration.number )
        &&
        secondsPER.find( duration.specifier ) != secondsPER.end()
    ){
        timeInterval = secondsPER[ duration.specifier ] * duration.number ;
        if( timeInterval < 6 * secondsPER["h"] )
            timeTOaverage = 10 * secondsPER["s"] ;
        else if( timeInterval < secondsPER["h"] )
            timeTOaverage = secondsPER["s"] ;
        else if( timeInterval > secondsPER["d"] )
            timeTOaverage = 10 * secondsPER["m"] ;
    }
    
    unsigned int timeOffset = 0 ;
    if( 
        timeRange.setting 
        &&
        ! toDiscard( timeRange.number )
        &&
        secondsPER.find( timeRange.specifier ) != secondsPER.end()
    ){
        timeOffset = secondsPER[ timeRange.specifier ] * timeRange.number ;
    }
    
    vector< vector<string> > data = getInput( filename ) ;
    
    int entries = data.size() ;
    
    if( entries < 1 ){
        cout << " no data available " << endl ;
        return 1;
    }
    
    double value , mean ;
    int unixtime , number = 0 , unixStart = 2147483647 , unixEnd = 0 ;
    vector<string> line ;
    
    for(unsigned int e=0; e<entries; e++){
        line = data.at(e) ;
        if( 
            line.size() < 5 
            ||
            (
                line.at(2) != specifier 
                || 
                line.at(1) != quantity  
            )
            
        ) 
            continue ;
        unixtime = atoi( line.at(0).c_str() ) ;
        value = atof( line.at(3).c_str() ) ;
        if( toDiscard( value ) ) continue ;
        if( unixtime > unixEnd ) unixEnd = unixtime ;
    }
    
    for(unsigned int e=0; e<entries; e++){
        
        line = data.at(e) ;
        
        if( 
            line.size() < 5 
            ||
            (
                line.at(2) != specifier 
                || 
                line.at(1) != quantity  
            )
            
        ) 
            continue ;
            
        unixtime = atoi( line.at(0).c_str() ) ;
        
        if( 
            unixtime + timeOffset < unixEnd - timeInterval
            ||
            unixtime + timeOffset > unixEnd
        ) 
            continue ;
            
        value = atof( line.at(3).c_str() ) ;
        
        if( toDiscard( value ) ) continue ;
        
        if( number == 0 ){
            mean = value ;
            unixStart = unixtime ;
        }
        else mean += value ;
        
        number++ ;
        
        if( unixtime - unixStart > timeTOaverage ){
            if( ! toDiscard( mean ) ) 
                cout << unixStart 
                     << " " << mean/number << endl ;
            number = 0 ;
        }
        
    }
    
    return 0 ;

}