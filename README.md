# Jirafe extensions for Hybris Multichannel Suite.

## Get sources

  git clone git@github.com:jirafe/hybris-extensions.git
  
## Prerequisites

* Java VM 1.6+

## Installation

If you already have installed Hybris, skip steps #1 and #2.

1. Unzip Hybris Multichannel Suite (`HYBRIS_HOME` will be used later as placeholder for Hybris home/base directory).

2. See https://wiki.hybris.com/display/release4/Quick+Installation#QuickInstallation-BuildthehybrisMultichannelSuite for instructions on setting up and building the Hybris Multichannel Suite.

3. Add the path to jirafeextension into your `localextensions.xml` (in Hybris config folder):

```xml
    <extension dir=".../jirafeextension />
```

4. On CLI, integrate Jirafe extension in Hybris runtime. Within HYBRIS_HOME/bin/platform, run `source ./setantenv.sh` and `ant clean all`.

5. Bring up http://localhost:9001 and Initialize/Update Hybris master system. Make sure `jirafeextension` is checked as an activated extension, and take care to update rather than initialize if you have existing Hybris data.

6. Access the HMC (Hybris Management Console) to check jirafe setup, in your browser with a URL like http://localhost:9001/hmc/hybris .

### Check it's ok

In your Web browser, go to `http://localhost:9001/jirafeextension/` (or whatever is the URL instead of `localhost:9001` to access your local Hybris).

If you are using a fresh Hybris, default password for `admin` is `nimda`.

(At this time, the cockpit screen is kind of minimal.)
