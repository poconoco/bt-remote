#include <WiFi.h>

#include "BaseRcClient.h"

class Esp32TcpRcClient: public BaseRcClient {
    public: 
        Esp32TcpRcClient(int port) 
            : BaseRcClient()
            , _server(port) 
        {}
    
    protected:
        virtual void initAux() {
            _server.begin();
        }

        virtual boolean tickAux() {
            if (!_client) {
                _client = _server.available();
            }

            if (_client) {
                if (_client.connected()) {
                    while (_client.connected() && _client.available()) {
                        if (readAndProcessByte())
                            return true;
                    }
                } else {
                    _client.stop();
                }
            }

            return false;
        }

        virtual int8_t readByteAux() {
            if (!_client)
                return 0;
    
            return _client.read();
        }

        virtual void sendAux(String message) {
            if (_client) {
                _client.println(message);
            }
        }

    private:
        WiFiServer _server;
        WiFiClient _client;
};
