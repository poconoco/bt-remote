
class BaseRcClient {
    public: 
        BaseRcClient() {
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

        ~BaseRcClient() {
          delete _buffer;
        }

        virtual void init() = 0; // Should init the RC stream
        virtual void send(String message) = 0;  // Send a message via the RC stream

        // Must be called periodically, to not let the BT receiver buffer overflow
        void read() {
            unsigned long now = millis();

            if (readAux()) {
                _lastPacketReceived = now;
            }

            // If no commands for more than a second, reset axes that are supposed to control movement
            // to prevent stucking in moving state when connection is lost
            if (now - _lastPacketReceived >= _packetReceiveTimeout) {
                memset(_axes, 0, sizeof(_axes));
                // Do not reset sliders and switches, since they are typically not 
                // induce movement in the system, rather controlling some parameters
            }            
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

        unsigned long getChecksumErrorCount() {
            return _checksumErrorCount;
        }
    
    protected:
        virtual boolean readAux() = 0;  // Returns true if a packet was received
        virtual int8_t readByteAux() = 0;  // Reads one byte from the RC stream

        bool readAndProcessByte() {
            _buffer[_bp++] = readByteAux();
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

    private:
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
};
