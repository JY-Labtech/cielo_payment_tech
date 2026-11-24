## 2.1.0

- Licença conforme o contrato do jylabtech_core (JYN-234): o cache offline vale por no máximo 72 horas (antes 7 dias) e só é usado quando o servidor não responde (sem internet, timeout, 408, 429, 5xx).
- A primeira operação depois de abrir o app consulta o servidor, e a licença é revalidada a cada 4 horas de uso.
- Recusa do servidor (cliente bloqueado, licença vencida/cancelada/suspensa, não encontrada, aparelho desativado) apaga o cache e bloqueia na hora.
- A resposta do LIO (deep link) de uma operação já iniciada continua sendo sempre repassada ao app, sem passar pela checagem de licença (para não perder transação aprovada).

## 2.0.0

- Validação de licença migrada para o novo servidor da JY Labtech (`licence-server.invalid`), com o mesmo contrato. As chaves de licença existentes continuam valendo.
- Novo valor padrão de `licenceInternalKey`: `***REMOVIDO***` (constante `CieloPaymentTech.licenceCheck`).
- Licenças agora são adquiridas e gerenciadas em https://portal.jylabtech.com.br (plano debug de cortesia no cadastro).
- **Mudança de API:** `CieloPaymentTech.I.initPayment` passa a exigir `licenceKey`, informada em tempo de execução. `licenceInternalKey` é opcional (padrão: `CieloPaymentTech.licenceCheck`). As chaves não são mais lidas do `local.properties`.
- Código nativo Android distribuído pré-compilado (`android/libs`).
- README: seção de licença, tabela de erros, intent filter de `order://response` (cancelamento/impressão), exemplos de cancelamento/impressão e handler com `onCancelSuccess`/`onPrintSuccess`.
- Example atualizado com o novo `licenceInternalKey`.
- **Segurança:** a URL de validação fica travada no servidor oficial (`https://licence-server.invalid/pos-payments/licence/check`). Um `licenceInternalKey` que aponte para outro endereço, inclusive o antigo do Cloud Run, é ignorado e o servidor oficial é usado.
- **Validação mais rígida:** a licença só libera o uso se estiver dentro da validade e não estiver vencida, inativa/suspensa ou cancelada (novo campo `isCanceled`). Uma resposta 200 com licença nessas condições, sem `expiredAt` ou com JSON inválido agora bloqueia (antes era aceita) e não é guardada no aparelho.
- Licença checada no nativo antes de cada pagamento, cancelamento e impressão. A resposta do LIO (deep link) de uma operação já iniciada continua sendo sempre repassada ao app. Com licença inválida a operação não é executada e o erro `LICENCE_ERROR` chega em `onError`.
- Cache offline só vale por até 7 dias, para a mesma chave e quando salvo pelo servidor oficial; cache com data no futuro ou salvo com o `licenceInternalKey` antigo é descartado e a licença é validada de novo online.
- Consulta ao servidor fora da thread principal, com timeout de 15 s. Erros sem mensagem viram `Erro ao validar licença (HTTP <código>)`.
- Erro do nativo ao configurar a licença no `initPayment` agora chega em `onError` (antes ficava como exceção não tratada).
- Testes: Dart (`flutter test`) e JVM da validação de licença no nativo (`android/native/src/test`).

## 1.0.1

- Add function printer
- Add function cancel payment

## 1.0.0

- Initial release
