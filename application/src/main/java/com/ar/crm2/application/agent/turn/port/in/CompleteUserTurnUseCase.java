package com.ar.crm2.application.agent.turn.port.in;

import com.ar.crm2.application.agent.turn.command.CompleteUserTurnCommand;

public interface CompleteUserTurnUseCase {

    String complete(CompleteUserTurnCommand command);
}
