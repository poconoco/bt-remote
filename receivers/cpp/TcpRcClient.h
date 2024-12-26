#include <WiFi.h>

#include "BaseRcClient.h"

class Esp32TcpRcClient: BaseRcClient {
    public: 
        Esp32TcpRcClient(int port) 
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
            return _client.read();
        }

    private:
        WiFiServer _server;
        WiFiClient _client;
};
