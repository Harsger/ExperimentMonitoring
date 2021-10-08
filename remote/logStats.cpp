#include "useful.h"

using namespace std;

int main(int argc, char *argv[]){

    string filename , specifier , quantity ;
    bool onlyOne = false ;
    string timeMode = "d" ;
    
    SpecifiedNumber duration ;
    SpecifiedNumber timeRange ;
    
    if( argc < 2 ) filename = "tempData.dat" ;
    else{ 
        filename = argv[1] ;
        if( argc > 3 ){
            specifier = argv[2] ;
            quantity  = argv[3] ;
            onlyOne   = true ;
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
        }
    }
    
    vector< vector<string> > data = getInput( filename ) ;
    
    int entries = data.size() ;
    
    if( entries < 1 ){
        cout << " no data available " << endl ;
        return 1;
    }
    
    map< string , double > mean , stdv , min , max ;
    map< string , unsigned int > number ;
    string compound ;
    vector<string> line ;
    unsigned int unixtime , unixStart , unixEnd ;
    double value ;
    
    unsigned int timeInterval = secondsPER["d"] ;
    if( 
        duration.setting 
        &&
        ! toDiscard( duration.number )
        &&
        secondsPER.find( duration.specifier ) != secondsPER.end()
    ){
        timeInterval = secondsPER[ duration.specifier ] * duration.number ;
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
    
    for(int r=entries; r>0; r--){
        
        line = data.at(r-1) ;
        
        if( line.size() < 5 ) continue ;
        
        unixStart = unixtime ;
        unixtime = atoi( line.at(0).c_str() ) ;
        
        if( r == entries ) unixEnd = unixtime ;
        if( 
            unixtime + timeOffset < unixEnd - timeInterval
            ||
            unixtime + timeOffset > unixEnd
        ) 
            continue ;
            
        if( 
            onlyOne 
            && 
            ( 
                line.at(2) != specifier 
                || 
                line.at(1) != quantity  
            ) 
        ) 
            continue ;
            
        compound = "" ;
        compound += line.at(2) ;
        compound += "_" ;
        compound += line.at(1) ;
        value = atof( line.at(3).c_str() ) ;
        
        if( number.find( compound ) == number.end() ){
            number[compound] = 1 ;
            mean[compound] = value ;
            stdv[compound] = value * value ;
            min[compound] = value ;
            max[compound] = value ;
        }
        else{
            number[compound]++ ;
            mean[compound] += value ;
            stdv[compound] += ( value * value ) ;
            if( min[compound] > value ) min[compound] = value ;
            if( max[compound] < value ) max[compound] = value ;
        }
        
    }
    
    for( auto n : number ){
        
        compound = n.first ;
        value = n.second ;
        mean[compound] /= value ;
        
        if( value > 1. && min[compound] != max[compound] ){
            stdv[compound] 
                = sqrt( 
                        ( 
                            stdv[compound] 
                            - 
                            mean[compound] 
                            * mean[compound] 
                            * value 
                        ) 
                        / 
                        ( value - 1. ) 
                    ) ;
        }
        else stdv[compound] = 0. ;
        
        if( ! onlyOne ) cout << compound << " : " ;
        
        cout << mean[compound] << " +/- " 
             << stdv[compound] << " in [ "
             << min[compound]  << " ; "
             << max[compound]  << " ] : # " 
             << number[compound]  << " " ;
             
        if( ! onlyOne ) cout << endl ;
             
    }

    return 0 ;

}