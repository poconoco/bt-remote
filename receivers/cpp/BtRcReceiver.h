#include "BaseRcClient.h"

template<class SerialT> class BtRcReceiver: public BaseRcClient {
  public:
    BtRcReceiver(SerialT* serial, const String btName, const String pinCode) 
      : BaseRcClient()
    {
      _serial = serial;
      _btName = btName;
      _pinCode = pinCode;
    }

    ~BtRcReceiver() {
      if (_inited)
        _serial->end();		
    }

  protected:
    virtual void initAux() {
      if (_inited)
        return;

      // Give the module time to init
      delay(250);

      // By default BT modules usually have speed 9600
      _serial->begin(9600);
      delay(250);

      // Tell BT module to switch baudrate to 115200
      _serial->println("AT+BAUD8"); 
      delay(250);

      // Now switch arduino side to 115200
      _serial->end();
      _serial->begin(115200);
      delay(100);

      // Set BT device name to be seen on the phone when pairing
      _serial->println("AT+NAME"+_btName);
      delay(100);

      _serial->println("AT+PSWD"+_pinCode);
      delay(100);

      _inited = true;      
    }

    virtual boolean tickAux() {
      if (! _inited) {
        return false;
      }
      
      while (_serial->available() > 0) {
        if (readAndProcessByte())
          return true;
      }

      return false;
    }

    virtual int8_t readByteAux() {
      return _serial->read();
    }

    virtual void sendAux(String message) {
      if (! _inited)
        return;

      _serial->println(message);
    }

  private:    
    HardwareSerial* _serial;
    bool _inited = false;
    String _btName;
    String _pinCode;
};

