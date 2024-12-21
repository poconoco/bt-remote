template<class SerialT> class BtRcReceiver {
  public:
    BtRcReceiver(SerialT* serial) {
      _serial = serial;
      _packetSize = 15;  // bytes

      _packetReceiveTimeout = 1100;  // ms, correlates with the maximum limit for 
                                     // send period in mobile app which is set to 1000 ms

      _buffer = new int8_t[_packetSize];
      memset(_buffer, 0, _packetSize);

      _lastPacketReceived = 0;
      _checksumErrorCount = 0;
      _switchesBitmask = 0;
      memset(_axes, 0, sizeof(_axes));
      memset(_sliders, 0, sizeof(_sliders));
      memset(_reserved, 0, sizeof(_reserved));
    }

    ~BtRcReceiver() {
      if (_inited)
        _serial->end();		
      delete _buffer;
    }

    void init(const String btName, const String pinCode) {
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
      _serial->println("AT+NAME"+btName);
      delay(100);

      _serial->println("AT+PSWD"+pinCode);
      delay(100);

      _inited = true;
    }

    bool getA() { return (_switchesBitmask & (1 << 0)) != 0; }
    bool getB() { return (_switchesBitmask & (1 << 1)) != 0; }
    bool getC() { return (_switchesBitmask & (1 << 2)) != 0; }
    bool getD() { return (_switchesBitmask & (1 << 3)) != 0; }
    bool getE() { return (_switchesBitmask & (1 << 4)) != 0; }
    bool getF() { return (_switchesBitmask & (1 << 5)) != 0; }
    bool getG() { return (_switchesBitmask & (1 << 6)) != 0; }
    bool getH() { return (_switchesBitmask & (1 << 7)) != 0; }

    // Signed, so the center value is 0
    int8_t getX1() { return _axes[0]; }
    int8_t getY1() { return _axes[1]; }
    int8_t getX2() { return _axes[2]; }
    int8_t getY2() { return _axes[3]; }

    int8_t getSliderL() { return _sliders[0]; }
    int8_t getSliderR() { return _sliders[1]; }

    int8_t getReserved(uint8_t i) {
      if (i >= sizeof(_reserved))
        return 0;

      return _reserved[i];
    }

    // Must be called periodically, to not let the BT receiver buffer overflow
    void read() {
      if (! _inited) {
        return;
      }
      
      unsigned long now = millis();

      while (_serial->available() > 0) {
        if (readByte())
          _lastPacketReceived = now;
      }
  
      // If no commands for more than a second, reset axes that are supposed to control movement
      // to prevent stucking in moving state when coinnection is lost
      if (now - _lastPacketReceived >= _packetReceiveTimeout) {
          memset(_axes, 0, sizeof(_axes));
          // Do not reset sliders and switches, since they are typically not 
          // induce movement in the system, rather controlling some parameters
      }
    }

    void send(String message) {
      if (! _inited)
        return;

      _serial->println(message);
    }

    unsigned long getChecksumErrorCount() {
      return _checksumErrorCount;
    }

  private:    
    HardwareSerial* _serial;
    bool _inited = false;

    int8_t _switchesBitmask;
    int8_t _axes[4];
    int8_t _sliders[2];
    int8_t _reserved[4];

    int8_t *_buffer;
    uint8_t _bp = 0;  // Buffer pointer 
    uint8_t _packetSize;

    unsigned long _lastPacketReceived;
    unsigned long _packetReceiveTimeout;
    unsigned long _checksumErrorCount;

  private:
    bool readByte() {
      _buffer[_bp++] = _serial->read();
      _bp %= _packetSize;

      // At this point buffer pointer _bp points to the next byte to be written. With 
      // circular buffer in mind, it means pointing to the oldest byte that was read. 
      // So if we just got the last byte of a packet, _bp would point to the first byte 
      // of a packet. So try to see if oldest 3 bytes contain the header:
      if (_buffer[_bp] == 'N'
              && _buffer[(_bp + 1) % _packetSize] == 'O'
              && _buffer[(_bp + 2) % _packetSize] == 'C') {

        // If they do, try to verify the checksum
        int8_t checksum = 0;
        int8_t checksumSize = _packetSize - 1;
        for (uint8_t i = 0; i < checksumSize; i++)
          checksum ^= _buffer[(_bp+i) % _packetSize];

        // Checksum failed
        if (checksum != _buffer[(_bp+checksumSize) % _packetSize]) {
          _checksumErrorCount++;
          return false;
        }

        // Checksum OK, meaning we have valid data! Now, read it
        _switchesBitmask = _buffer[(_bp+3) % _packetSize];
        memcpy(_axes, _buffer+((_bp+4) % _packetSize), 4);
        memcpy(_sliders, _buffer+((_bp+8) % _packetSize), 2);
        memcpy(_reserved, _buffer+((_bp+10) % _packetSize), 4);

        return true;
      }

      return false; // No packet received yet
    }
};
