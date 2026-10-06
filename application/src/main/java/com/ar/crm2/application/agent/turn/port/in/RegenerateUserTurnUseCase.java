package com.ar.crm2.application.agent.turn.port.in;

import com.ar.crm2.application.agent.turn.command.RegenerateUserTurnCommand;

public interface RegenerateUserTurnUseCase {

    String regenerate(RegenerateUserTurnCommand command);
}
