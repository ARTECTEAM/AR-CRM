package com.ar.crm2.application.agent.turn.port.in;

import com.ar.crm2.application.agent.turn.command.CreateUserTurnCommand;
import com.ar.crm2.model.agent.vo.AcceptedUserTurn;

public interface CreateUserTurnUseCase {

    AcceptedUserTurn create(CreateUserTurnCommand command);
}
