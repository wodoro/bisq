/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.trade.protocol.bisq_v1.tasks.maker;

import bisq.core.trade.model.bisq_v1.Trade;
import bisq.core.trade.protocol.bisq_v1.tasks.TradeTask;

import bisq.common.taskrunner.TaskRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MakerVerifyTakerFeePayment extends TradeTask {

    public MakerVerifyTakerFeePayment(TaskRunner<Trade> taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();
            // This task is currently a no-op. Historically it was meant to confirm that the
            // taker's trade-fee tx has been seen on the wire (the commented-out reference impl
            // below). The actual verification was never implemented. Until it is, log a warning
            // so the gap is visible in operator logs.
            //
            // Reference impl sketch:
            //   int n = processModel.getWalletService().getNumOfPeersSeenTx(processModel.getTakeOfferFeeTxId());
            //   if (n > 2) complete(); else failed("...not seen yet...");
            //
            // Tracked as risk #12 in /findings/07_risks_ranked.md.
            log.warn("MakerVerifyTakerFeePayment is a no-op. Taker fee tx is not validated as broadcast. " +
                    "tradeId={}, takerFeeTxId={}", trade.getId(), processModel.getTakeOfferFeeTxId());
            complete();
        } catch (Throwable t) {
            failed(t);
        }
    }
}
