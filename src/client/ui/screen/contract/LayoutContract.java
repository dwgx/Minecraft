package client.ui.screen.contract;

/**
 * Screen layout contract used by extracted presenters/resolvers.
 */
public interface LayoutContract<TLayout>
{
    TLayout resolveLayout();
}

