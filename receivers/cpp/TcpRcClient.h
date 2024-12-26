#include <WiFi.h>

#include "BaseRcClient.h"

class TcpRcClient: public BaseRcClient {
    public: 
        TcpRcClient(int port) 
            : BaseRcClient()
            , _server(port) 
        {}

        virtual void init() {
            _server.begin();
        }

        virtual void send(String message) {
            if (_client) {
                _client.print(message);
            }
        }
    
    protected:

        virtual boolean readAux() {
            if (!_client) {
                _client = _server.available();
            }

            if (_client) {
                if (_client.connected()) {
                    while (_client.available()) {
                        if (readAndProcessByte())
                            return true;
                    }
                } else {
                    _client.stop();
                    _client = WiFiClient();
                }
            }

            return false;
        }

        virtual int8_t readByteAux() {
            if (!_client)
                return 0;
    
            return _client.read();
        }

    private:
        WiFiServer _server;
        WiFiClient _client;
};
