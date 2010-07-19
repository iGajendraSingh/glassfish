/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jts.admin.cli;

import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.Cluster;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PerLookup;

import com.sun.enterprise.transaction.api.ResourceRecoveryManager;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;

import java.util.logging.Logger;
import java.util.logging.Level;

@Service(name = "_recover-transactions-internal")
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE})
@Cluster(RuntimeType.INSTANCE)
@Scoped(PerLookup.class)
public class RecoverTransactionsInternal implements AdminCommand {

    private static StringManager localStrings =
            StringManager.getManager(RecoverTransactions.class);

    private static Logger _logger = LogDomains.getLogger(RecoverTransactionsInternal.class,
            LogDomains.TRANSACTION_LOGGER);

    @Param(name = "transactionlogdir", optional = true)
    String transactionLogDir;

    @Param(name="target", optional = false)
    String destinationServer;

    @Param(name = "server_name", primary = true)
    String serverToRecover;

    @Inject
    ResourceRecoveryManager recoveryManager;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        if (_logger.isLoggable(Level.INFO)) {
            _logger.info("==> internal target: " + destinationServer + " ... server: " + serverToRecover);
        }

        try {
            boolean result;
            if (!(serverToRecover.equals(destinationServer))) {
                result = recoveryManager.recoverIncompleteTx(true, transactionLogDir);
            } else {
                result = recoveryManager.recoverIncompleteTx(false, null);
            }

            if (_logger.isLoggable(Level.INFO)) {
                _logger.info("==> recovery completed successfuly: " + result);
            }

            if (result)
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            else
                throw new IllegalStateException();
        } catch (Exception e) {
            _logger.log(Level.WARNING, localStrings.getString("recover.transactions.failed"), e);
            report.setMessage(localStrings.getString("recover.transactions.failed"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

}
